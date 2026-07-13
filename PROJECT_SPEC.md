# LunarLog Product and Technical Specification

Last verified against the source: July 13, 2026.

## Purpose

LunarLog is a privacy-first Android app for menstrual cycle and related wellness tracking. Core tracking works offline, requires no account, and stores health data locally. Predictions are estimates, not medical advice or contraception guidance.

## Supported Product Behavior

- Record, edit, and remove period ranges with overlap and date validation.
- Log symptoms, custom symptoms, moods, flow, water, sleep, sleep quality, libido, notes, temperature, and cervical mucus by date and time.
- Add daily, weekly, or as-needed medications; optionally schedule private reminders; record doses by date.
- Show history, exact symptom filtering, calendar summaries, and future period/fertile-day estimates.
- Calculate cycle statistics from start-to-next-start intervals and only from completed cycles.
- Use temperature and cervical-mucus observations only when enough consecutive, plausible data exists; calendar fertility remains clearly labelled as an estimate.
- Generate paginated PDF summaries and detailed CSV exports.
- Create and restore bounded, validated JSON backups, including user-selectable reminder/theme preferences. App-lock state is deliberately not restored.
- Offer optional Android device-credential/biometric app lock, notification reminders, a Glance widget, and GitHub-build updates.

LunarLog is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Users should consult a healthcare professional for medical advice, diagnosis, or treatment. Fertile-day estimates are not birth control.

## Privacy and Security Model

- No account, advertising SDK, analytics SDK, or crash-reporting SDK.
- Room and DataStore are the local sources of truth.
- Android cloud/device-transfer backup is excluded; exports happen only through a user-selected document destination.
- Sensitive app screens block screenshots and recents thumbnails.
- Notifications use private visibility and generic lock-screen public versions.
- App lock gates content until its saved state is loaded and uses monotonic elapsed time for background-timeout decisions.
- Restore validates size, structure, identifiers, dates, enums, and foreign-key relationships before replacing existing data.

## Architecture

- Kotlin/JDK 17, Jetpack Compose Material 3, Hilt, Coroutines/Flow, Room, WorkManager, Navigation Compose, and Glance.
- `core/model`: persisted cycle and aggregate log models.
- `data`: entities, DAOs, repositories, converters, backup payloads, and Room migrations.
- `logic`: deterministic prediction, analysis, scheduling, and narrative logic.
- `ui`: Compose screens and Hilt ViewModels.
- `workers`: private cycle, daily-log, and medication reminders.
- Database version: 9. Schema exports are committed and migrations are tested from the oldest publicly released database version (8).

## Distribution

- `play`: Play Store bundle; GitHub sideload updater disabled and internet/install-package permissions omitted.
- `github`: signed sideload APK; update checks use GitHub Releases.
- Stable and prerelease SemVer tags are distinguished by the release workflow. Every published tag syncs its Android version metadata so subsequent APKs have increasing version codes; GitHub's stable `latest` endpoint does not offer prereleases to normal update checks.

## Quality Gates

- Pull requests and `main` builds run unit tests, Android lint, and both debug distribution assemblies.
- CodeQL analyzes Java/Kotlin on pull requests, `main`, and weekly.
- Dependabot tracks Gradle and GitHub Actions updates.
- Room schema export and migration tests protect persisted user data.
- Release tags must be valid SemVer and point to the current `origin/main` HEAD.

## Remaining Product Roadmap

- Expand instrumentation coverage beyond migration and smoke flows.
- Consider modularizing pure logic only if build or reuse needs justify it.
- Any future cloud sync, partner sharing, PIN implementation, or regulated medical functionality requires a new privacy/security design and must not be represented as currently available.
