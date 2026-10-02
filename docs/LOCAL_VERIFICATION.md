# Local verification — 2026-10-02

Environment: Android 15 / API 35 AOSP x86_64 emulator, with a 320dp-wide display. This cloud machine has no KVM acceleration. Software emulation caused severe startup delays and an Android `system_server` crash; these runs cannot measure app performance.

## Issues found and fixed

- Room 2.8.4 migration tests failed with `AbstractMethodError` in `GeneratedSerializer.typeParametersSerializers()`. AGP had pinned the test APK to the app's transitive serialization 1.7.3 runtime, although Room's migration library requests 1.8.1. A shared serialization BOM now aligns both APKs to 1.8.1.
- The real database backup round-trip test caught lost `dosesPerDay` and `reminderTimes`. Both export and restore mappings now retain them; older backups still default to one dose and no additional reminder list.

## Verification

- After the fixes, `test lintPlayDebug lintGithubDebug assemblePlayDebug assembleGithubDebug :app:assembleGithubDebugAndroidTest` passed: 161 JVM tests per flavor (322 executions), no failures/errors/skips. Both lint tasks passed with existing non-blocking warnings.
- All 10 Node release-helper tests passed.
- Android database/PDF rerun passed: **11 tests**, zero failures, including migrations 8→9→10→11, both rollback paths, multi-dose medication/backup round trips, reset/search, legacy sleep undo and multi-page PDF rendering. [Raw runner results](verification/2026-10-02/android-instrumentation.txt).
- Visually inspected [page 2 of the Android-generated PDF](verification/2026-10-02/report-page-2.png): body text remains readable after the page break, long unbroken names wrap inside the margins, and the footer does not overlap the body. The PNG was rendered from the pulled PDF onto white paper. The test screenshot helper was subsequently given an explicit white background; this artifact-only adjustment was compiled, not rerun on Android.
- Interactive Compose checks were attempted, but Android aborted the run with `INSTRUMENTATION_ABORTED: System has crashed`. Activity recreation, rotation and 200% text layout are not signed off by this run. Their test sources compile and are included in CI.

Gradle device discovery timed out on this software emulator. Tests were instead launched using `adb shell am instrument -w -r` and `androidx.test.runner.AndroidJUnitRunner`. The emulator's app-debug setting was enabled to avoid Android killing the process solely for slow startup. These adjustments affect this disposable emulator, not the app or CI configuration.

Real-device biometric lock flows, TalkBack, background reminder delivery, widget interaction and installer permission flows remain on the [QA checklist](QA_CHECKLIST.md).

## Subsequent UI polish

The [UI polish pass](UI_POLISH.md) passed `test lintPlayDebug lintGithubDebug assemblePlayDebug assembleGithubDebug :app:assembleGithubDebugAndroidTest`: 322 JVM test executions with no failures, errors or skips, including the theme contrast regressions. Both lint checks and both debug APK builds passed. Updated Compose test sources compiled. The new layouts were not executed on a device; the earlier 11 Android database/PDF tests predate this UI pass.

## Release-readiness follow-up

- After fixing notification-link replay on activity recreation, the full debug test/lint/build command passed again: 322 JVM executions, zero failures/errors/skips. Updated navigation instrumentation compiled but requires the remote device run.
- `assembleGithubRelease bundlePlayRelease` passed locally. These are build checks, not production signing or upgrade-install verification.
- The 10 Node release-helper tests passed again. `actionlint` 1.7.7 accepted both CI and release workflows; shell syntax and a simulated failing device run confirmed diagnostic collection does not hide a test failure.
- CI now supports normal and compact/200%-text profiles, saves device diagnostics, and is reused as a prerequisite for publication. The Git connection supports pushing the review branch; CLI/API connectivity is unavailable.
- See [candidate readiness](release/candidate/READINESS.md) and [draft release notes](release/candidate/RELEASE_NOTES.md). Physical-device and accessibility checks remain unsigned off.
