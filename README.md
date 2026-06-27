<div align="center">
  <img src="docs/assets/banner.png" alt="Voela - Pull sound apart" width="100%" />
</div>

<br/>

> [!IMPORTANT]
> Please leave a ⭐ if you like this project

<br/>

Voela is an on-device Android audio toolkit. Extract audio from YouTube, separate vocals
from the instrumental, and cut audio into clips - all processed **locally on your device**.
No cloud, no uploads, no account, no limits.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue)
![Version](https://img.shields.io/badge/version-1.1.1-8B5CF6)
![On-device](https://img.shields.io/badge/processing-100%25%20on--device-34D399)

## Demo

![Voela demo](docs/assets/voela-demo.gif)

> [**Watch the full-quality demo (MP4)**](https://github.com/itsvaibhavmishra/Voela/raw/main/docs/assets/voela-demo.mp4)

## Features

- **Split Vocals** - separate any track into **vocals** and **instrumental** on-device, with
  a **Fast** engine for quick results and a **Best** engine (DTTNet) for sharper quality.
- **YouTube extraction** - paste a link and pull the audio out, saved to `Music/Voela/YouTube Downloads`.
- **Split Audio** - cut a track into equal-length clips with live preview.
- **Trim** - select the exact range before processing.
- **Library & Recents** - your extractions and splits are tracked in an app-private library
  with per-item and total size, plus optional auto-clear.
- **Theme** - pick from six dark-mode accent colours (Purple, Blue, Teal, Emerald, Amber, Rose),
  each with contrast-safe text. Dark mode throughout.
- **Output formats** - choose the format and quality for your stems (M4A, MP3, WAV).

## Screens

| Home | YouTube extraction | Choose a feature |
| :---: | :---: | :---: |
| <img src="docs/assets/screenshots/home.jpg" width="230"> | <img src="docs/assets/screenshots/youtube.jpg" width="230"> | <img src="docs/assets/screenshots/feature.jpg" width="230"> |
| **Trim** | **Split Audio** | **Separation results** |
| <img src="docs/assets/screenshots/trim.jpg" width="230"> | <img src="docs/assets/screenshots/split-audio.jpg" width="230"> | <img src="docs/assets/screenshots/results.jpg" width="230"> |

## Privacy

Everything happens on your device. Audio is never uploaded, and the app works fully offline
once a track is on your phone. Exported files land in your own **Music/Voela** folder; the
working library is kept app-private.

## 💻 Tech Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![C / JNI](https://img.shields.io/badge/C%20%2F%20JNI-A8B9CC?style=for-the-badge&logo=c&logoColor=black)
![ONNX Runtime](https://img.shields.io/badge/ONNX%20Runtime-005CED?style=for-the-badge&logo=onnx&logoColor=white)
![Media3 ExoPlayer](https://img.shields.io/static/v1?style=for-the-badge&message=Media3+ExoPlayer&color=3DDC84&logo=android&logoColor=white&label=)
![WorkManager](https://img.shields.io/static/v1?style=for-the-badge&message=WorkManager&color=4285F4&logo=android&logoColor=white&label=)
![KissFFT](https://img.shields.io/static/v1?style=for-the-badge&message=KissFFT+STFT&color=222222&label=)
![LAME](https://img.shields.io/static/v1?style=for-the-badge&message=LAME+MP3&color=000000&label=)
![yt-dlp](https://img.shields.io/badge/yt--dlp-FF0000?style=for-the-badge&logo=youtube&logoColor=white)

Native vocal separation runs on ONNX Runtime with a KissFFT STFT/iSTFT pipeline (JNI/C). Single-activity Compose, Navigation Compose, targets `arm64-v8a` · minSdk 26 · compileSdk 36 · dark mode only.

## Build

Requirements: JDK 17, Android SDK 36, NDK 28.2.x.

```bash
./gradlew assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/`. Local builds fall back to the
debug signing key; release builds in CI are signed with a release keystore (see below).

## Releases & versioning

The app version lives in [`version.properties`](version.properties) (`VERSION_NAME` /
`VERSION_CODE`) - the single source of truth, read by Gradle.

- **Cut a release:** bump `version.properties`, open a PR to `main`, and merge. CI builds and
  publishes a GitHub Release tagged `v<VERSION_NAME>` with the APK (it skips if that tag exists).
- **`staging`** acts as a sandbox: every push rebuilds and updates a rolling `staging`
  pre-release, so any pipeline failure surfaces before it reaches `main`.

Download the latest build from the [**Releases**](https://github.com/itsvaibhavmishra/Voela/releases) page.

## License

All rights reserved.

<br/>

<div align="center">
<img src="https://komarev.com/ghpvc/?username=itsvaibhavmishra&&style=for-the-badge" align="center" />
</div>

<br/>

---
