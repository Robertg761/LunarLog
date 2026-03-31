# LunarLog

LunarLog is an Android app for menstrual cycle and wellness tracking with a local-first privacy model.

## What It Does

- Tracks cycle start and end dates
- Supports daily logs for symptoms, mood, flow, sleep, hydration, and notes
- Provides calendar and analysis views for trends and predictions
- Includes period history and detail screens for edits and corrections
- Generates report-friendly exports and supports local backup/restore
- Offers app lock, reminders, app widget support, and in-app update checks

## Core Principles

- Privacy first: no required account and no cloud dependency
- Offline ready: core usage does not depend on network
- Full feature set: advanced insights without paywalls

## Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- MVVM architecture
- Hilt for dependency injection
- Room (SQLite) for persistence
- Coroutines and Flow
- WorkManager for scheduled notifications
- Navigation Compose

## Requirements

- Android Studio with Android SDK 34
- JDK 17
- Android device or emulator (API 26+)

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync finish.
4. Run the `app` configuration on a device/emulator.

## Command Line Build and Test

```bash
./gradlew :app:assembleDebug
./gradlew :app:bundlePlayRelease
./gradlew :app:assembleGithubRelease
./gradlew test
```

## Project Structure

- `app/src/main/java/com/lunarlog`: application code
- `app/src/main/java/com/lunarlog/ui`: Compose screens and UI components
- `app/src/main/java/com/lunarlog/data`: Room entities, DAOs, repositories, and data management
- `app/src/main/java/com/lunarlog/logic`: prediction, anomaly detection, and insight logic
- `app/src/main/java/com/lunarlog/update`: GitHub release update check and APK install flow
- `app/src/test/java/com/lunarlog`: unit tests
- `scripts`: release helper scripts
- `.github/workflows/release.yml`: signed APK release automation
- `docs/play-store-release.md`: Play Store release checklist and signing setup

## Versioning and Releases

- App version is defined in `app/build.gradle.kts` (`versionCode`, `versionName`).
- `CHANGELOG.md` is the source for release notes.
- `playRelease` builds the Play Store `.aab` bundle.
- `githubRelease` builds the signed sideload APK used by GitHub Releases.
- The release workflow builds a signed `githubRelease` APK and publishes a GitHub Release when `main` is updated with version/changelog changes.

## Testing

Current unit tests cover key logic and repository behaviors, including cycle prediction, reminder policy, narrative generation, period summaries, and selected ViewModels.

Run all unit tests:

```bash
./gradlew test
```

## Security and Privacy Notes

- `android:allowBackup` is disabled in the manifest.
- App lock and biometric flow are supported.
- Data backup/restore is handled locally by the app.
- Update checks query GitHub Releases and install from release APK assets.

## Related Docs

- `PROJECT_DESC.md`: short project summary
- `PROJECT_SPEC.md`: feature roadmap and implementation phases
- `CHANGELOG.md`: release history
