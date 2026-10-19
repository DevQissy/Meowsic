# Meowsic

**Your music, your sound.**

Meowsic is an Android music player developed by Dev Qissy, combining music library management, customizable audio effects, and playback controls in one application.

## Features

### Music Library
- Create and manage playlists.
- Keep track of favorite songs.
- Browse and organize music by folders and albums.

### Audio Effects
- Equalizer presets and manual equalizer controls.
- Bass booster.
- Spatial audio enhancement.
- Audio amplifier.
- Nine echo modes.

### Playback
- Sleep timer.
- Adjustable playback speed.
- Optional volume-button media controls, including shortcuts for previous and next tracks.

## Technical Information

| Property | Value |
|---|---|
| Application ID | `com.devqissy.meowsic` |
| Version | 1.1 |
| Version Code | 3 |
| Minimum Android | Android 9 (API 28) |
| Target SDK | API 36 |
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Developer | Dev Qissy |

## Installation

### Install a Release APK

1. Visit the [GitHub Releases](https://github.com/DevQissy/Meowsic/releases) page.
2. Download the APK from a published release.
3. Open it on your Android device and follow the installation prompts.

Only install APKs from sources you trust.

### Build from Source

1. Clone or download this repository.
2. Open the project in Android Studio.
3. Allow Gradle to sync the project.
4. Build and run the application.

On Windows:

```bat
gradlew.bat assembleDebug
```

On Linux or macOS:

```bash
./gradlew assembleDebug
```

The debug APK is normally generated at:

`app/build/outputs/apk/debug/`

## Privacy

Review the source code and requested permissions to understand how Meowsic accesses music files and uses device features. Do not assume that the application is entirely offline without reviewing its implementation.

## License

Original source code owned by Dev Qissy is subject to the terms in [LICENSE](LICENSE).

Third-party components retain their respective licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Disclaimer

Meowsic is an independently developed application. All third-party names and trademarks belong to their respective owners.

The application is provided "AS IS", to the extent permitted by applicable law.
