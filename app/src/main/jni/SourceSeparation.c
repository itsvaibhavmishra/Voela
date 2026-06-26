// Thin JNI shim over the sherpa-onnx C-API for offline source separation.
// sherpa-onnx ships no Kotlin binding for separation, so we dlopen the C-API at
// runtime and call it. Plain C (no STL) so we add no libc++ dependency.
//
// The engine is created once (nativeCreate) and reused across chunks
// (nativeProcess), so a long track is processed in bounded memory without
// reloading the model each time. Each process call returns the two stems as
// interleaved float arrays for the caller to stream to disk.
#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define TAG "SherpaSep"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

// Struct layouts mirror sherpa-onnx/c-api/c-api.h exactly (arm64 ABI).
typedef struct { const char *vocals; const char *accompaniment; } SpleeterCfg;
typedef struct { const char *model; } UvrCfg;
typedef struct {
    SpleeterCfg spleeter;
    UvrCfg uvr;
    int32_t num_threads;
    int32_t debug;
    const char *provider;
} ModelCfg;
typedef struct { ModelCfg model; } SSConfig;
typedef struct { float **samples; int32_t num_channels; int32_t n; } Stem;
typedef struct { const Stem *stems; int32_t num_stems; int32_t sample_rate; } SSOutput;

typedef const void *(*CreateFn)(const SSConfig *);
typedef void (*DestroyFn)(const void *);
typedef const SSOutput *(*ProcessFn)(const void *, const float *const *, int32_t, int32_t, int32_t);
typedef void (*DestroyOutFn)(const SSOutput *);

static void *lib(void) { return dlopen("libsherpa-onnx-c-api.so", RTLD_NOW | RTLD_GLOBAL); }

JNIEXPORT jlong JNICALL
Java_com_vaibhawmishra_voela_data_audio_SourceSeparator_nativeCreate(
    JNIEnv *env, jobject thiz, jstring jVocalsModel, jstring jAccompModel, jstring jUvrModel,
    jstring jProvider, jint numThreads) {
    void *h = lib();
    if (!h) { LOGE("dlopen failed: %s", dlerror()); return 0; }
    CreateFn create = (CreateFn)dlsym(h, "SherpaOnnxCreateOfflineSourceSeparation");
    if (!create) { LOGE("dlsym create failed"); return 0; }

    const char *vModel = (*env)->GetStringUTFChars(env, jVocalsModel, NULL);
    const char *aModel = (*env)->GetStringUTFChars(env, jAccompModel, NULL);
    const char *uModel = (*env)->GetStringUTFChars(env, jUvrModel, NULL);
    const char *provider = (*env)->GetStringUTFChars(env, jProvider, NULL);

    SSConfig cfg;
    memset(&cfg, 0, sizeof(cfg));
    if (uModel && strlen(uModel) > 0) {
        cfg.model.uvr.model = uModel;
    } else {
        cfg.model.spleeter.vocals = vModel;
        cfg.model.spleeter.accompaniment = aModel;
    }
    cfg.model.num_threads = numThreads;
    cfg.model.debug = 0;
    cfg.model.provider = provider;

    const void *ss = create(&cfg);

    (*env)->ReleaseStringUTFChars(env, jVocalsModel, vModel);
    (*env)->ReleaseStringUTFChars(env, jAccompModel, aModel);
    (*env)->ReleaseStringUTFChars(env, jUvrModel, uModel);
    (*env)->ReleaseStringUTFChars(env, jProvider, provider);
    if (!ss) LOGE("create engine failed (provider=%s)", provider);
    return (jlong)(intptr_t)ss;
}

JNIEXPORT jobjectArray JNICALL
Java_com_vaibhawmishra_voela_data_audio_SourceSeparator_nativeProcess(
    JNIEnv *env, jobject thiz, jlong handle,
    jfloatArray jInterleaved, jint numChannels, jint numFrames, jint inRate) {
    if (!handle) return NULL;
    void *h = lib();
    if (!h) return NULL;
    ProcessFn process = (ProcessFn)dlsym(h, "SherpaOnnxOfflineSourceSeparationProcess");
    DestroyOutFn destroyOut = (DestroyOutFn)dlsym(h, "SherpaOnnxDestroySourceSeparationOutput");
    if (!process || !destroyOut) { LOGE("dlsym process failed"); return NULL; }

    jfloat *in = (*env)->GetFloatArrayElements(env, jInterleaved, NULL);
    int32_t nch = numChannels, n = numFrames;
    float **planar = (float **)malloc(sizeof(float *) * nch);
    for (int c = 0; c < nch; c++) planar[c] = (float *)malloc(sizeof(float) * n);
    for (int i = 0; i < n; i++)
        for (int c = 0; c < nch; c++) planar[c][i] = in[i * nch + c];
    (*env)->ReleaseFloatArrayElements(env, jInterleaved, in, JNI_ABORT);

    const SSOutput *out = process((const void *)(intptr_t)handle, (const float *const *)planar, nch, n, inRate);

    for (int c = 0; c < nch; c++) free(planar[c]);
    free(planar);

    jobjectArray result = NULL;
    if (out && out->num_stems >= 1) {
        LOGI("num_stems=%d rate=%d", out->num_stems, out->sample_rate);
        int32_t count = out->num_stems > 2 ? 2 : out->num_stems;
        jclass faCls = (*env)->FindClass(env, "[F");
        result = (*env)->NewObjectArray(env, count, faCls, NULL);
        for (int s = 0; s < count; s++) {
            int32_t sn = out->stems[s].n, snch = out->stems[s].num_channels;
            jfloatArray arr = (*env)->NewFloatArray(env, sn * snch);
            float *tmp = (float *)malloc(sizeof(float) * sn * snch);
            for (int i = 0; i < sn; i++)
                for (int c = 0; c < snch; c++) tmp[i * snch + c] = out->stems[s].samples[c][i];
            (*env)->SetFloatArrayRegion(env, arr, 0, sn * snch, tmp);
            free(tmp);
            (*env)->SetObjectArrayElement(env, result, s, arr);
            (*env)->DeleteLocalRef(env, arr);
        }
    } else {
        LOGE("process returned no stems");
    }
    if (out) destroyOut(out);
    return result;
}

JNIEXPORT void JNICALL
Java_com_vaibhawmishra_voela_data_audio_SourceSeparator_nativeDestroy(JNIEnv *env, jobject thiz, jlong handle) {
    if (!handle) return;
    void *h = lib();
    if (!h) return;
    DestroyFn destroy = (DestroyFn)dlsym(h, "SherpaOnnxDestroyOfflineSourceSeparation");
    if (destroy) destroy((const void *)(intptr_t)handle);
}
