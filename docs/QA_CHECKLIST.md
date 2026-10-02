# Current release QA

Run against the candidate version in `app/build.gradle.kts`; this checklist is not a release sign-off. Android 9/10 App Lock compatibility is outside this remediation's scope at the user's request.

## Automated gates

- [x] `./gradlew test lintPlayDebug lintGithubDebug assemblePlayDebug assembleGithubDebug`
- [x] `node --test scripts/tests/*.test.js`
- [ ] `./gradlew connectedGithubDebugAndroidTest` on API 35; CI now supplies an emulator.
- [x] Check schema 8→9→10→11 migration, dose retention, real transaction rollback, mood search, backup round trip, and reset reseeding. Executed on API 35 via the direct Android test runner; [results and limits](LOCAL_VERIFICATION.md).

## Device and accessibility checks

- [ ] API 30+ App Lock enable/unlock/cancel; a dirty logging or medication draft survives rotation, background/relock/unlock, and process recreation.
- [ ] At 320dp width, landscape, and 200% font size: date pickers, medication schedule chips, dose counts, reports, restore preview and confirmation remain scrollable and actionable.
- [ ] TalkBack and keyboard navigation: every form field, switch, slider and dose action has an understandable label and focus order; errors are discoverable.
- [ ] Leave Home/Calendar open across midnight; change time zone and resume. Current date and open-period coverage update without a database write.
- [ ] Tap cycle/medication notifications from a cold start and while locked; reach the intended calendar/day after authentication.
- [ ] Create two daily doses with two reminders, then weekly and as-needed medications. Log/remove individual doses, edit schedules and archive/resume. Confirm historical doses survive edits and archival.
- [ ] Verify reminders before/after each logged dose, after reboot/time-zone change, and when notification permission is denied. Android may delay background work; reminder times are not exact alarms.
- [ ] Widget redaction hides health details and disables stale quick actions; medication widget counts match the daily screen. Regional calendar headers and grid agree.
- [ ] Restore preview shows the correct counts/date span. Cancel keeps current data; successful restore creates a usable previous-data backup. A failed backup/write must prevent replacement. Reset deletes the recovery backup and reseeds choices immediately.
- [ ] CSV/PDF export to a slow document provider: UI remains responsive, failures show feedback, selected ranges and dose timestamps are correct. Inspect multi-page PDF body font and long-word wrapping.
- [ ] Explicit dry/zero observations remain distinct from missing entries; notes-only days never confirm a mucus peak. Compare Celsius/Fahrenheit exports and mixed-scale temperature observations.
- [ ] GitHub release build: manual version check, offline error, download cancellation/retry, unknown-source permission denial, and Android installer rejection. Play builds continue to use store distribution.

Do not mark device checks complete from JVM tests or APK compilation alone. Existing version-specific checklists under `docs/release/` are historical records.
