# LunarLog

<p align="center">
  <img src="docs/assets/lunarlog-logo-512.png" alt="LunarLog logo" width="128">
</p>

LunarLog is an Android app for menstrual cycle and wellness tracking with a local-first privacy model.

## What It Does

- Tracks cycle start and end dates
- Supports daily logs for symptoms, mood, flow, sleep, hydration, and notes
- Provides calendar and analysis views for trends and predictions
- Tracks daily, weekly, and as-needed medications with optional private reminders
- Includes period history and detail screens for edits and corrections
- Generates report-friendly exports and supports local backup/restore
- Offers app lock, reminders, app widget support, and in-app update checks

## Core Principles

- Privacy first: no required account and no cloud dependency
- Offline ready: core usage does not depend on network
- Full feature set: advanced insights without paywalls

Cycle and fertile-day predictions are estimates. LunarLog is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Consult a healthcare professional for medical advice, diagnosis, or treatment; fertile-day estimates are not birth control.

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

- Android Studio with Android SDK 35
- JDK 17
- Android device or emulator (API 26+)

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync finish.
4. Run the `app` configuration on a device/emulator.

## Command Line Build and Test

```bash
./gradlew :app:assembleGithubDebug
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
- `CHANGELOG.md` is used for release notes when it has a matching section for the pushed tag.
- `playRelease` builds the Play Store `.aab` bundle.
- `githubRelease` builds the signed sideload APK used by GitHub Releases.
- The release workflow runs when a stable or prerelease SemVer tag is pushed to the current `main` HEAD. It tests and builds a signed `githubRelease` APK, marks prereleases correctly, and syncs each version bump back to `main` so every installable release gets a higher Android `versionCode`.

## Play Console Release Documentation

LunarLog has separate Play Store and GitHub release channels:

- Google Play: `playRelease` creates an Android App Bundle without internet or sideload-updater permissions.
- GitHub Releases: `githubRelease` creates the direct-download APK with the GitHub updater flow enabled.

Play Console package and store listing details:

- Package name: `com.lunarlog`
- App category/positioning: privacy-first menstrual cycle and wellness tracking
- Privacy policy URL: `https://robertg761.github.io/LunarLog/`
- Store logo source: `docs/assets/lunarlog-logo-512.png`
- Local Play Store asset source, when present: `local/play-store-assets/`

Submission notes:

- Upload `app/build/outputs/bundle/playRelease/app-play-release.aab` to Internal testing before wider rollout.
- Complete the Health apps declaration with both `Period Tracking` and `Medication and Treatment Management`.
- Complete the Data safety form to match LunarLog's local-first behavior: no account requirement, no advertising SDKs, no third-party analytics/crash-reporting SDKs, and cycle/wellness data stored on-device unless the user exports or shares it.
- Keep store listing copy, screenshots, icon, feature graphic, notification text, and health-related claims aligned with the privacy policy and actual app behavior.

## Testing

Automated checks cover cycle prediction, fertility-signal validation, reminders, narratives, correlations, reporting, backup/restore, repository aggregation, themes, selected ViewModels, and database migration. CI also runs Android lint, assembles both debug distributions, and performs CodeQL analysis.

Run all unit tests:

```bash
./gradlew test
```

## Security and Privacy Notes

- `android:allowBackup` is disabled in the manifest.
- Android cloud/device-transfer extraction rules exclude app data.
- App screens use secure-window protection and private notification visibility.
- App lock and biometric flow are supported.
- Data backup/restore is handled locally by the app.
- Update checks query GitHub Releases and install from release APK assets.

## Related Docs

- `PROJECT_DESC.md`: short project summary
- `PROJECT_SPEC.md`: verified product and technical specification
- `CHANGELOG.md`: release history
- `docs/play-store-release.md`: Play release build, signing, upload, and channel notes
- `docs/google-play-compliance-checklist.md`: Play policy and data safety checklist
- `docs/privacy-policy.md`: privacy policy for store listing and public publishing

## License

LunarLog is available under the [MIT License](LICENSE).
