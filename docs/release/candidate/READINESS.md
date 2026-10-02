# Candidate readiness

Status: **not ready to publish**. Changes are prepared on the `codex/audit-remediation-qa` review branch. Version name/code remain 1.10.2 / 28; assign a new version only after verification, before publication. No tag, release or deployment has been created.

## Evidence and outstanding work

Local follow-up checks passed: 322 JVM executions, both debug APKs and lint, instrumentation compilation, GitHub release APK and Play release bundle builds, 10 Node tests, and actionlint workflow validation. Production signing and upgrade installation remain unverified.

See [local verification](../../LOCAL_VERIFICATION.md), [audit changes](../../AUDIT_REMEDIATION.md), [UI polish](../../UI_POLISH.md), and the [device checklist](../../QA_CHECKLIST.md).

- Prior local Android run: 11 database/PDF tests passed. That run predates the latest UI/lifecycle changes.
- Publication now waits for the reusable CI workflow, including both device-test profiles; a failed or skipped quality gate blocks the publish job. The CI concurrency group includes the workflow name so a normal CI run cannot cancel a release check.
- Current automated CI definition: normal API 35 and compact 320dp / 200% text runs; the navigation test also rotates to landscape. Both runs retain test reports, generated test artifacts and logcat. These tests do not provide TalkBack, biometric, widget-host or document-provider sign-off.
- The Git connection supports a review-branch push, which triggers CI. CLI/API access is unavailable (the API proxy rejects the connection); CI results must be checked through the Actions page. No device is attached and this machine has no KVM acceleration; the previous software-emulator UI run ended with an Android system-process crash.
- The follow-up code review fixed notification-link replay after recreation. Pending links, including ones waiting behind App Lock/onboarding, are saved with activity state; consumed links remain consumed. The existing navigation device test now checks navigation away from a handled link before recreation.

- Hosted verification found and fixed a restored logging sheet that could hide its save/error footer, and a Home-tab back-stack restoration bug after notification navigation. The navigation regression covers the production notification intent, returning Home, activity recreation, rotation and saving a retained draft. Its harness follows resumed activities because Android's ActivityScenario stops observing when the activity changes its intent.
- CI keeps the test APK installed until evidence collection finishes, preserving generated screenshots and PDFs along with test reports. The Actions result for the branch's current commit is the source of truth for full device-test completion.

## Completion sequence

1. Let CI and both device profiles finish on the review branch’s exact commit. Review retained artifacts, resolve failures, and open a PR using the branch comparison page if API access remains unavailable.
2. Complete the remaining physical-device checklist: keyboard/insets, TalkBack focus and announcements, lock/unlock/cancel/relock, notification permissions and delivery, widget privacy/actions, actual backup/restore/export providers, and update installer flows.
3. Review the full diff and schema/backup changes. Assign the next version name and monotonically increasing version code, then rerun required checks for that candidate.
4. Build with the existing production signing identity, verify installation over the currently published version using synthetic data, and confirm old data and dose history remain intact. Use the repository's guarded release workflow for publication after readiness is confirmed.

## Rollback

Prefer a forward fix for any issue after schema 11 is installed. An older APK expecting schema 10 cannot safely open the upgraded database; do not treat an APK downgrade as a rollback. Retain a pre-upgrade backup in a format the previous release supports if testing a downgrade in an isolated test environment. A new format-3 recovery backup is intended for the new app and is not a guarantee of compatibility with an older release.

Never clear a user's data to work around a migration failure. Preserve the database and backup, reproduce with synthetic data, and prepare a compatible corrective migration or release.
