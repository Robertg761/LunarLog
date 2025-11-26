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
    *   **`workers/`**: Background workers (`CycleNotificationWorker`).

## 🚀 Building & Running
*   **Build Debug APK:** `./gradlew assembleDebug`
*   **Build Release APK:** `./gradlew assembleRelease`
*   **Run Unit Tests:** `./gradlew test`
*   **Run Instrumented Tests:** `./gradlew connectedAndroidTest`

## 📝 Development Conventions

### Update & Release Workflow
1.  **Code Updates:** Update code and bump `versionName`/`versionCode` in `app/build.gradle.kts`.
2.  **Changelog:** Update `CHANGELOG.md` with new version details.
3.  **Commit:** Commit and push changes.
4.  **Release Draft:**
    *   Extract specific text for the new version from `CHANGELOG.md`.
    *   Draft GitHub Release (vX.X.X) using this text.
5.  **Publishing (User Task):**
    *   Manually sign the APK.
    *   Upload signed APK to the release.
    *   Publish.

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
