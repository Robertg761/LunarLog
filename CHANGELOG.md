# Changelog

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
