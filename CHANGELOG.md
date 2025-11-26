# Changelog

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
