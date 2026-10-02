# Audit remediation

Scope: findings from the audit of `fddb36884e24118e2e85dcbc680a4a0cf76baf91`, with Android 9/10 compatibility excluded by the user. The user explicitly approved changing medication tracking to multiple doses per day. Release version metadata is unchanged.

## Data and recovery

- Batch logging commits entries and the daily summary in one transaction. Failed saves keep the sheet and its draft; concurrent submission is disabled. Lazy legacy hydration is also transactional.
- Shared validation rejects invalid typed values, non-finite measurements and dates outside 1900 through ten years ahead. New sleep additions cannot exceed 24 hours per day; existing excessive totals can be corrected. Granular entries are authoritative when rebuilding legacy backup summaries.
- Backup readers and writers share a 10 MiB/100,000-record limit and validation. Export fails explicitly rather than producing a file that the app cannot restore. Users with oversized histories must reduce the data or export reports; streaming backups beyond this limit remain unsupported.
- Restore validates before replacement, previews record counts and dates, and saves a private pre-restore file atomically. Settings can export that file for recovery. The file survives until the next restore or reset; it is excluded from Android backup and contains sensitive readable data. Database replacement is transactional; DataStore preferences are a separate storage operation.
- Reset reseeds default symptom/mood choices and cancels reminders/notifications. It also removes the recovery file.

## Correctness and daily use

- Onboarding supports skipping unknown history and explicitly choosing ongoing versus ended periods. It no longer infers an end from the supplied start.
- Symptom learning uses period-start to next-period-start intervals, including post-bleeding observations. Home uses the most recent two years of logs for suggestions.
- Mood filtering queries mood entries. History keeps archived choices available for filtering.
- Cycle and medication notifications have explicit navigation intents. Date-dependent Home/Calendar state refreshes on resume and within 30 seconds of midnight or a time-zone change.
- Logging and medication fields are saveable across recreation; the unlocked navigation state is retained while app lock is displayed. Dirty drafts prompt before dismissal. Entry deletions have undo.
- Explicit zero/none values are stored as entries instead of being silently removed.
- Estimated period ends are identified in history, details, calendar previews and CSV; editing the date confirms/corrects the estimate. Home displays the amount/variability of prediction evidence.

## Medication redesign

Database version 11 adds archival, expected doses per scheduled day and multiple reminder times, and removes the unique medication/day log constraint. Existing records retain IDs, dates, taken flags and timestamps. The full migration chain remains 8→9→10→11.

- Daily and weekly schedules support 1–24 doses, with individually chosen reminder times. As-needed doses are recorded individually without automatic reminders.
- Each event has its own time and can be removed independently after confirmation. Logging beyond the planned count remains possible so actual history is representable; this is not dosing advice.
- Existing records default to one planned dose. Editing uses UPDATE rather than REPLACE so foreign-key cascades cannot erase history. Archive hides scheduled choices while preserving historical records.
- Reminders compare the number of logged doses with the reminder's position in the day's schedule. This does not match a dose to a medically prescribed time window. Users select their own schedule; Android background-work timing is approximate.
- Widgets display actual/planned dose counts and open the day for dose entry. They no longer toggle/delete a whole day's history.
- Backups now write version 3 and still read legacy/version 2. Older app versions do not understand the new dose format; do not downgrade a populated database.

## Reports, privacy and UI

- CSV/PDF work runs on IO with progress and error feedback. Ranges include month, six months, year and all time. Exports include medication dose history and explicit temperature scales; CSV retains estimated-date provenance.
- PDF wrapping restores body style after page breaks and splits overlong words.
- Retrospective temperature-shift and mucus-peak observations are exposed with uncertainty text and exact-value rows. Missing mucus readings cannot count as three lower observations; explicitly recorded dry readings can. These observations do not alter future predictions or confirm ovulation.
- Custom choices can be renamed or archived. Internal keys remain stable and historical wording is preserved.
- Optional widget redaction hides health information and blocks quick actions. Settings explains that unredacted widgets expose health details independently of app lock.
- Medication controls wrap/scroll; common UI strings are in Android resources, dates and calendar week starts follow locale, and temperature input accepts decimal comma. This is resource preparation, not a claim of completed translations; dynamic narrative strings remain English. Legacy temperatures preserve their unambiguous Celsius/Fahrenheit numeric scale and are normalized for BBT comparisons.
- Calendar computes bounded display windows rather than expanding historical intervals. Calendar and range-based analysis queries fetch the requested date windows; all-time reports intentionally read all history.
- Flow-backed screens expose load/write failures and preserve coroutine cancellation. GitHub updater adds manual checks, cancel/retry, and installer/start errors while retaining Android signature enforcement.

## Verification

Unit regressions cover invalid values, sleep correction, phase boundaries, missing observations, onboarding, date rollover, dose scheduling/counts, and report contents. Instrumentation covers real Room batch rollback, restore rollback, backup round trips with legacy excessive sleep and multiple doses, archival-safe edits, reset and mood search. CI now runs the instrumentation suite on an API 35 emulator.

See [current QA checklist](QA_CHECKLIST.md) for device-only checks. Building an instrumentation APK does not execute its tests; rotation, TalkBack, layout, real notifications and installer behavior require device verification. Consult the task's final check results for what was actually executed locally.

### Local results (2026-10-02)

- Final combined Gradle command succeeded: `test lintPlayDebug lintGithubDebug assemblePlayDebug assembleGithubDebug :app:assembleGithubDebugAndroidTest`.
- 161 JVM tests per flavor, 322 executions total; zero failures, errors or skips.
- Both lint tasks passed, with seven non-blocking warnings per flavor (target/dependency updates, existing layout API/style advisories, and resource ellipsis typography).
- All 10 Node release-helper tests passed.
- Executed the actual 10→11 migration SQL against desktop SQLite: ordinary-table columns, defaults and indexes match schema 11; old history remains and multiple doses survive medication edits.
- A subsequent local API 35 emulator run exposed two issues: Room migration helpers were pinned to an incompatible serialization runtime, and medication backup mappings omitted dose count/reminder times. The app/test serialization runtimes are now aligned to 1.8.1, and both backup mappings preserve these fields.
- After these fixes, the combined Gradle verification passed again (322 JVM executions, both APKs and lint); all 10 Node tests also passed again. See [local device verification](LOCAL_VERIFICATION.md) for Android execution results and limitations.
- `git diff --check` passed. No release version bump, commit, push or deployment was performed.
