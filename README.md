# Voela

**Pull sound apart.**

Voela is an on-device Android audio toolkit. Extract audio from YouTube, separate
vocals from the instrumental, and cut audio into clips. Everything runs locally on
your device: no cloud, no uploads, no account.

## Features

- **Split Vocals** - separate any track into vocals and instrumental on-device, with
  a Fast engine for quick results and a Best engine for sharper quality.
- **YouTube extraction** - paste a link and pull the audio out, saved straight to Music.
- **Split Audio** - cut a track into equal-length clips and save them as a set.
- **Trim** - select the exact range you want before processing.
- **Library and Recents** - your extractions and splits are tracked in an app-private
  library with per-item and total size, plus optional auto-clear.
- **Format control** - choose the output format and quality for your stems.

## Privacy

All audio processing happens on the device. Files are never uploaded, and the app
works fully offline once a track is on your phone. Exported files are written to your
own Music folder; the working library is kept app-private.

## Tech

- Kotlin and Jetpack Compose (Material 3)
- Media3 ExoPlayer and Transformer for playback and transcoding
- WorkManager for background processing
- Native vocal separation: ONNX Runtime with a KissFFT STFT/iSTFT pipeline (JNI/C)
- Bundled LAME for MP3 export
- youtubedl-android for YouTube extraction

## Build

Requirements: JDK 17, Android SDK 36, NDK 28.2.x. The app targets `arm64-v8a`.

```bash
./gradlew assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/`.

## License

All rights reserved.
