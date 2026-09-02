# LunarLog

**Mission:** To build a privacy-first, offline, and feature-complete menstrual cycle tracker for Android. LunarLog aims to provide "premium" insights and features (PDF reports, advanced analytics, wellness tracking) completely free and without data collection.

## 🛠 Technical Stack
*   **Language:** Kotlin
*   **UI:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM (Clean Architecture)
*   **Dependency Injection:** Hilt
*   **Database:** Room (SQLite) with FTS4 support
*   **Asynchrony:** Coroutines & Flow
*   **Navigation:** Jetpack Navigation Compose
*   **Charts:** Vico
*   **Background Tasks:** WorkManager
*   **Widgets:** Glance

## 📂 Project Structure
*   **`app/src/main/java/com/lunarlog/`**
    *   **`data/`**: Entities (`Cycle`, `DailyLog`), DAOs, Repositories, `AppDatabase`.
    *   **`logic/`**: Pure Kotlin business logic (predictions, math, `NarrativeGenerator`).
    *   **`ui/`**: Composable screens, ViewModels, Theme.
        *   `analysis/`, `calendar/`, `home/`, `logdetails/`, `loghistory/`, `settings/`
    *   **`di/`**: Hilt modules (`AppModule`).
    *   **`workers/`**: WorkManager reminders (`CycleNotificationWorker`, `PeriodLogReminderWorker`, `MedicationReminderWorker`), their shared `NotificationChannels` / `NotificationIntents`, and `NotificationWorkScheduler`.
    *   **`update/`**: GitHub Releases update check, `SemVer`, and `ApkUpdateManager` (github flavor only).

## 🚀 Building & Running
*   **Build Debug APK (GitHub flavor):** `./gradlew :app:assembleGithubDebug`
*   **Build Release APK (GitHub sideload):** `./gradlew :app:assembleGithubRelease`
*   **Build Play Bundle:** `./gradlew :app:bundlePlayRelease`
*   **Run Unit Tests:** `./gradlew test`
*   **Run Lint (as CI does):** `./gradlew lintPlayDebug lintGithubDebug`
*   **Run Instrumented Tests:** `./gradlew connectedGithubDebugAndroidTest`

The `distribution` flavor dimension (`play` / `github`) means the bare `assembleDebug` and
`assembleRelease` task names do not exist; always name a flavor.

## 📝 Development Conventions

### Update & Release Workflow (ONLY IF INSTRUCTED TO CREATE A NEW RELEASE)
1.  **Code Updates:** Update code and bump `versionName`/`versionCode` in `app/build.gradle.kts`.
2.  **Changelog:** Update `CHANGELOG.md` with new `## [x.y.z] - YYYY-MM-DD` section details.
3.  **Commit:** Commit and push changes.
4.  **Publish:**
    *   If CI signing secrets are configured, merging to `main` auto-builds a signed release APK and publishes a GitHub Release `vX.Y.Z` with release notes from `CHANGELOG.md`.
    *   Otherwise, sign and publish manually (APK signing + upload + publish).

### key Files
*   **`PROJECT_SPEC.md`**: Detailed roadmap and feature specifications.
*   **`rules.md`**: Project-specific rules and context.
*   **`app/build.gradle.kts`**: App-level build configuration and dependencies.
*   **`gradle/libs.versions.toml`**: Dependency version catalog.

### Testing
*   Unit tests are located in `app/src/test`.
*   Instrumented tests are located in `app/src/androidTest` (currently minimal).
*   Use `Mockk` for mocking and `kotlinx-coroutines-test` for coroutine testing.

@rules.md
