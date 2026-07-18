# Changelog

## [1.8.1] - 2026-07-17

### Changed
- **Faster Flow Logging**: Add Log now opens directly on Flow and shows Flow as the first entry type, while edits still open on the existing entry's type.
- **Release Automation**: Pushing a validated version bump to `main` now automatically creates the matching tag and GitHub Release.

## [1.8.0] - 2026-07-13

### Added
- **Medication Tracking**: Added daily, weekly, and as-needed medications with optional private reminders and dose history.
- **Richer Daily Logs**: Added timed entries, custom symptoms, temperature, cervical-mucus observations, sleep quality, hydration, libido, and improved exact filtering.
- **Safer Data Tools**: Added detailed CSV reports plus bounded, validated JSON backup and transactional restore for logs, medications, and preferences.

### Changed
- **Predictions and Insights**: Cycle statistics now use start-to-next-start intervals from completed cycles, fertility estimates require more conservative evidence, and insights avoid overstating sparse data.
- **Reports and Navigation**: Improved PDF pagination, report completeness, responsive layouts, deep-link handling, and daily-log editing flows.
- **Privacy**: Sensitive screens now block screenshots and recents previews; notifications use private visibility with generic lock-screen text; Android cloud and device-transfer backup are disabled.

### Fixed
- **Database Safety**: Added the Room 8-to-9 migration, committed schema history, validated foreign-key relationships, and made restore replacement atomic.
- **Reminder Reliability**: Fixed scheduling and rescheduling for cycle, daily-log, and medication reminders, including reboot/time-change recovery.
- **Update Safety**: Hardened SemVer parsing, APK download validation, install-state handling, and stable-versus-prerelease selection.
- **Build and Maintenance**: Added CI, CodeQL, Dependabot, lint policy, immutable workflow action pins, and removed committed build artifacts and unused code.

## [1.7.9] - 2026-05-11

### Fixed
- **Home Period Counters**: Main screen now shows days until the next period as the large counter and days since the last period in the smaller summary.

## [1.7.8] - 2026-04-26

### Added
- **Calendar Day Preview**: Tapping a calendar date now opens a bottom-sheet preview with cycle status, flow, symptoms, mood, notes, and an edit action.

### Changed
- **Calendar Legend**: Clarified period flow intensity with light-to-heavy guidance and a less crowded legend layout.
- **Calendar Predictions**: Predicted period ranges now render as connected dashed pills for better range readability.

## [1.7.7] - 2026-04-26

### Changed
- **Page Transitions**: Replaced heavier tab slide/scale motion with a shorter fade transition for smoother page changes.
- **Analysis Performance**: Moved Insights aggregation work off the main thread to reduce navigation animation contention.
- **Launcher Icon**: Refined the adaptive icon foreground so it fits better inside Samsung launcher masks.

## [1.7.6] - 2026-04-26

### Fixed
- **Launcher Icon**: Updated the installed Android launcher icon to use the Play Store logo.
- **Update Notes**: Prevented GitHub Release logo markup from appearing as raw text in the in-app updater.
- **Release Workflow**: Removed embedded HTML from generated release notes while keeping the logo attached as a release asset.

## [1.7.4] - 2026-04-25

### Fixed
- **Calendar Prediction Continuation**: Prevented just-ended long periods from appearing to continue into the following day as a predicted period.
- **Countdown Accuracy**: Adjusted next-period countdowns after a period ends so they account for the recorded end date.

## [1.7.3] - 2026-03-31

### Changed
- **Release Versioning**: Bumped Play Store release metadata to a new version code for a fresh upload attempt.

## [1.7.2] - 2026-03-31

### Changed
- **Play Store Packaging**: Added a dedicated Play release channel that removes sideload-only updater behavior and produces a Play-ready Android App Bundle.
- **Target SDK Compliance**: Updated the Play release path to target Android 15 / API level 35 for current Google Play submission requirements.

## [1.7.1] - 2026-02-17

### Changed
- **Home Counter Model**: Reworked the main counter to countdown-first behavior: estimated period days left while ongoing, then estimated days until next period after period end.
- **Cross-Surface Consistency**: Unified counter semantics across Home, widget, and share status text using a shared counter presentation calculator.

### Fixed
- **Stale Update Prompt**: Prevented the "Update downloaded, install now" prompt from appearing after the update was already installed and the app was reopened.
- **Overdue Visibility**: Counter now shows overdue states explicitly instead of collapsing overdue and due states into "due today".
- **Period Overage Clarity**: Active periods that exceed estimate now display overage days instead of ambiguous/negative countdown behavior.

## [1.7.0] - 2026-02-17

### Added
- **Security Model**: Introduced `AppLockMode` with lock timeout policy options (`Now`, `30s`, `2m`) and stronger device-auth handling.
- **Notification Control**: Added explicit user opt-in for cycle prediction alerts in Settings.
- **Calendar Accessibility**: Added TalkBack-friendly day semantics and a "Today" jump control.
- **Release Operations**: Added Stage 6 artifacts under `docs/release/1.7.0/` (release notes, rollout plan, rollback playbook, QA smoke checklist, release-readiness report).

### Changed
- **Period Domain API**: Replaced ambiguous period toggling with explicit, validated operations (`startPeriod`, `endOngoingPeriod`, `resumePeriodEndedOn`, `setPeriodDay`, `setPeriodRange`, `updateCycleDates`) and typed results.
- **Data Consistency**: Unified logging writes around transactional granular entries; aggregate `DailyLog` is now rebuilt from entries to prevent source-of-truth drift.
- **Onboarding Reliability**: Onboarding completion is now success-gated with explicit error state handling.
- **Analysis Reactivity**: Analysis data now updates reactively instead of loading once per screen init.
- **Search Robustness**: Added FTS query sanitization for safer note search behavior.

### Fixed
- **Cycle Integrity**: Prevented invalid period mutations (including `endDate < startDate`) when editing historical days.
- **Collector Lifecycle**: Fixed `LogListViewModel` date-loading collector duplication.
- **Destructive UX**: Added confirmation for ending an active period from Quick Log and clearer invalid-save affordances in period logging.

## [1.6.1] - 2026-02-15

### Changed
- Streamlined in-app updates: LunarLog now guides users through download, "allow installs from this app", and installation without sending them to GitHub.
- Update checks are disabled for debug builds to avoid signature mismatch install failures.

## [1.6.0] - 2026-02-15

### Changed
- Automated GitHub Release publishing on merges to `main` (release notes pulled from `CHANGELOG.md` and signed APK attached via CI).
- Release signing can be configured via environment variables for CI builds.

## [1.5.0] - 2026-01-16

### Added
- **Period History View**: New "Periods" tab in bottom navigation to view all recorded periods in a list format (most recent first).
- **Period Details**: Tap any period to view and edit start/end dates, see logged symptoms for each day, or delete the period.
- **Period Management**: FAB in Period History to log new periods quickly.

### Fixed
- **Period Toggle Bug**: Fixed race condition where toggling a period day wouldn't update the UI correctly.

---

## [1.4.1] - 2026-01-06

### Added
- **Retroactive Period Logging**: Tap any past date in Calendar to mark it as a period day with the new Period toggle switch.
- **Version Display**: Settings now shows app version in the About section.

### Fixed
- **Crash on Startup**: Fixed theme incompatibility crash (Theme.AppCompat required by AppCompatActivity).
- **Duplicate Resources**: Removed duplicate `Theme.LunarLog` definition that caused release build failures.

## [0.1.4] - 2025-11-26

### Fixed
- **Reports**: Resolved issue where PDF/CSV exports were saved to hidden private storage. Now uses the System File Picker (Storage Access Framework) to allow users to choose the save location (e.g., Downloads, Documents).

### Changed
- **Architecture**: Moved `Cycle` and `DailyLog` models to `com.lunarlog.core.model` for better modularity.
- **Architecture**: Moved `Converters` to `com.lunarlog.data` and refactored to use Gson for safer serialization.
- **Safety**: Removed `fallbackToDestructiveMigration()` from production database configuration to prevent accidental data loss.

### Added
- Enabled Compose compiler metrics for performance benchmarking (optional via `-PenableComposeCompilerMetrics=true`).

## [0.1.3] - 2025-11-26

### Changed
- Switched in-app update mechanism to check GitHub Releases directly instead of Gist JSON.
- Updated repository URL to `Robertg761/LunarLog`.

## [0.1.1] - 2025-11-25

### Added
- In-app update mechanism using `AppUpdater` library.
- "Update Available" indicator (badge) on the Settings icon in Home screen.
- "Update Available" card in Settings screen to trigger installation.
- Visual confirmation (Snackbar) when toggling period status (Start/End).
- Dynamic "End Period" button logic (Start -> End -> Period Ended/Resume).

### Fixed
- Crash when clicking "End Period" caused by main thread database access (moved to background thread).
- Crash on startup caused by incorrect Theme/Activity inheritance (migrated to `AppCompatActivity` and `Theme.AppCompat`).
- UI state not updating correctly when ending a period on the current day.

### Changed
- Bumped version code to 2 and version name to 0.1.1.
