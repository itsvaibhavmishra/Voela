// On-device DTTNet vocal separation: STFT (KissFFT) -> ONNX Runtime -> iSTFT.
// Reuses the libonnxruntime.so already bundled by sherpa-onnx (dlopen'd at runtime,
// no extra dependency). Processes one fixed CHUNK (261120 samples, stereo) per call.
// STFT/iSTFT reproduce torch.stft exactly (verified to ~1e-6 against golden vectors).
#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include "kiss_fftr.h"
#include "onnxruntime_c_api.h"

#define TAG "Dttnet"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

#define N_FFT 6144
#define HOP 1024
#define DIM_F 2048
#define DIM_T 256
#define N_BINS 3073   // N_FFT/2 + 1
#define PAD 3072      // N_FFT/2
#define CHUNK 261120  // HOP*(DIM_T-1)
#define PADDED (CHUNK + 2 * PAD)
#define TENSOR (4 * DIM_F * DIM_T)

typedef struct {
    const OrtApi *ort;
    OrtEnv *env;
    OrtSession *session;
    OrtMemoryInfo *mem;
    kiss_fftr_cfg fwd;
    kiss_fftr_cfg inv;
    float window[N_FFT];
} Engine;

static const OrtApi *g_ort = NULL;

static int init_ort(void) {
    if (g_ort) return 1;
    void *h = dlopen("libonnxruntime.so", RTLD_NOW | RTLD_GLOBAL);
    if (!h) { LOGE("dlopen libonnxruntime.so failed: %s", dlerror()); return 0; }
    const OrtApiBase *(*base)(void) = (const OrtApiBase *(*)(void))dlsym(h, "OrtGetApiBase");
    if (!base) { LOGE("dlsym OrtGetApiBase failed"); return 0; }
    g_ort = base()->GetApi(ORT_API_VERSION);
    return g_ort != NULL;
}

JNIEXPORT jlong JNICALL
Java_com_vaibhawmishra_voela_data_audio_DttSeparator_nativeCreate(JNIEnv *env, jobject thiz, jstring jModel) {
    if (!init_ort()) return 0;
    const OrtApi *ort = g_ort;
    Engine *e = (Engine *)calloc(1, sizeof(Engine));
    e->ort = ort;
    for (int n = 0; n < N_FFT; n++) e->window[n] = 0.5f - 0.5f * cosf(2.0f * (float)M_PI * n / N_FFT);
    e->fwd = kiss_fftr_alloc(N_FFT, 0, NULL, NULL);
    e->inv = kiss_fftr_alloc(N_FFT, 1, NULL, NULL);

    OrtStatus *st = ort->CreateEnv(ORT_LOGGING_LEVEL_WARNING, "dtt", &e->env);
    if (st) { LOGE("CreateEnv: %s", ort->GetErrorMessage(st)); ort->ReleaseStatus(st); free(e); return 0; }
    OrtSessionOptions *opts;
    ort->CreateSessionOptions(&opts);
    ort->SetIntraOpNumThreads(opts, 4);
    ort->SetSessionGraphOptimizationLevel(opts, ORT_ENABLE_ALL);

    const char *model = (*env)->GetStringUTFChars(env, jModel, NULL);
    st = ort->CreateSession(e->env, model, opts, &e->session);
    (*env)->ReleaseStringUTFChars(env, jModel, model);
    ort->ReleaseSessionOptions(opts);
    if (st) { LOGE("CreateSession: %s", ort->GetErrorMessage(st)); ort->ReleaseStatus(st); free(e); return 0; }

    ort->CreateCpuMemoryInfo(OrtArenaAllocator, OrtMemTypeDefault, &e->mem);
    LOGI("DTTNet engine ready");
    return (jlong)(intptr_t)e;
}

// reflect-pad x[CHUNK] into out[PADDED] (numpy 'reflect': mirror without repeating edge)
static void reflect_pad(const float *x, float *out) {
    for (int i = 0; i < PAD; i++) out[i] = x[PAD - i];
    memcpy(out + PAD, x, CHUNK * sizeof(float));
    for (int j = 0; j < PAD; j++) out[PAD + CHUNK + j] = x[CHUNK - 2 - j];
}

// STFT one channel -> write real/imag into packed tensor at channel offsets
static void stft_ch(Engine *e, const float *x, float *tensor, int ch_real, int ch_imag,
                    float *padded, float *frame, kiss_fft_cpx *freq) {
    reflect_pad(x, padded);
    for (int t = 0; t < DIM_T; t++) {
        const float *src = padded + t * HOP;
        for (int k = 0; k < N_FFT; k++) frame[k] = src[k] * e->window[k];
        kiss_fftr(e->fwd, frame, freq);
        float *re = tensor + (size_t)ch_real * DIM_F * DIM_T;
        float *im = tensor + (size_t)ch_imag * DIM_F * DIM_T;
        for (int b = 0; b < DIM_F; b++) {
            re[b * DIM_T + t] = freq[b].r;
            im[b * DIM_T + t] = freq[b].i;
        }
    }
}

// iSTFT one channel (from tensor channel offsets) -> out[CHUNK]
static void istft_ch(Engine *e, const float *tensor, int ch_real, int ch_imag, float *out,
                     float *frame, kiss_fft_cpx *freq, float *acc, float *wsum) {
    memset(acc, 0, PADDED * sizeof(float));
    memset(wsum, 0, PADDED * sizeof(float));
    const float *re = tensor + (size_t)ch_real * DIM_F * DIM_T;
    const float *im = tensor + (size_t)ch_imag * DIM_F * DIM_T;
    for (int t = 0; t < DIM_T; t++) {
        for (int b = 0; b < N_BINS; b++) {
            if (b < DIM_F) { freq[b].r = re[b * DIM_T + t]; freq[b].i = im[b * DIM_T + t]; }
            else { freq[b].r = 0.f; freq[b].i = 0.f; }
        }
        kiss_fftri(e->inv, freq, frame);
        int off = t * HOP;
        for (int k = 0; k < N_FFT; k++) {
            float w = e->window[k];
            acc[off + k] += (frame[k] / N_FFT) * w;  // kiss inverse is unnormalized -> /N_FFT
            wsum[off + k] += w * w;
        }
    }
    for (int i = 0; i < CHUNK; i++) {
        float ws = wsum[PAD + i];
        out[i] = ws > 1e-10f ? acc[PAD + i] / ws : 0.f;
    }
}

JNIEXPORT jfloatArray JNICALL
Java_com_vaibhawmishra_voela_data_audio_DttSeparator_nativeProcess(JNIEnv *env, jobject thiz, jlong handle,
                                                               jfloatArray jInterleaved) {
    if (!handle) return NULL;
    Engine *e = (Engine *)(intptr_t)handle;
    const OrtApi *ort = e->ort;

    jfloat *in = (*env)->GetFloatArrayElements(env, jInterleaved, NULL);
    float *L = (float *)malloc(CHUNK * sizeof(float));
    float *R = (float *)malloc(CHUNK * sizeof(float));
    for (int i = 0; i < CHUNK; i++) { L[i] = in[2 * i]; R[i] = in[2 * i + 1]; }
    (*env)->ReleaseFloatArrayElements(env, jInterleaved, in, JNI_ABORT);

    float *tensor = (float *)calloc(TENSOR, sizeof(float));
    float *padded = (float *)malloc(PADDED * sizeof(float));
    float *frame = (float *)malloc(N_FFT * sizeof(float));
    kiss_fft_cpx *freq = (kiss_fft_cpx *)malloc(N_BINS * sizeof(kiss_fft_cpx));
    stft_ch(e, L, tensor, 0, 1, padded, frame, freq);
    stft_ch(e, R, tensor, 2, 3, padded, frame, freq);

    int64_t shape[4] = {1, 4, DIM_F, DIM_T};
    OrtValue *input = NULL, *output = NULL;
    ort->CreateTensorWithDataAsOrtValue(e->mem, tensor, (size_t)TENSOR * sizeof(float), shape, 4,
                                        ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT, &input);
    const char *in_names[] = {"spec"};
    const char *out_names[] = {"out"};
    OrtStatus *st = ort->Run(e->session, NULL, in_names, (const OrtValue *const *)&input, 1, out_names, 1, &output);

    jfloatArray result = NULL;
    if (st) {
        LOGE("Run: %s", ort->GetErrorMessage(st));
        ort->ReleaseStatus(st);
    } else {
        float *outData = NULL;
        ort->GetTensorMutableData(output, (void **)&outData);
        float *lo = (float *)malloc(CHUNK * sizeof(float));
        float *ro = (float *)malloc(CHUNK * sizeof(float));
        float *acc = (float *)malloc(PADDED * sizeof(float));
        float *wsum = (float *)malloc(PADDED * sizeof(float));
        istft_ch(e, outData, 0, 1, lo, frame, freq, acc, wsum);
        istft_ch(e, outData, 2, 3, ro, frame, freq, acc, wsum);
        result = (*env)->NewFloatArray(env, 2 * CHUNK);
        float *interleaved = (float *)malloc(2 * CHUNK * sizeof(float));
        for (int i = 0; i < CHUNK; i++) { interleaved[2 * i] = lo[i]; interleaved[2 * i + 1] = ro[i]; }
        (*env)->SetFloatArrayRegion(env, result, 0, 2 * CHUNK, interleaved);
        free(interleaved); free(lo); free(ro); free(acc); free(wsum);
    }

    if (output) ort->ReleaseValue(output);
    if (input) ort->ReleaseValue(input);
    free(tensor); free(padded); free(frame); free(freq); free(L); free(R);
    return result;
}

JNIEXPORT void JNICALL
Java_com_vaibhawmishra_voela_data_audio_DttSeparator_nativeDestroy(JNIEnv *env, jobject thiz, jlong handle) {
    if (!handle) return;
    Engine *e = (Engine *)(intptr_t)handle;
    if (e->mem) e->ort->ReleaseMemoryInfo(e->mem);
    if (e->session) e->ort->ReleaseSession(e->session);
    if (e->env) e->ort->ReleaseEnv(e->env);
    kiss_fftr_free(e->fwd);
    kiss_fftr_free(e->inv);
    free(e);
}
