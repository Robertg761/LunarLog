# Draft release notes

Version and publication date: not assigned. This is an unreleased working-tree candidate.

- Track multiple medication doses per day, with individual dose times, planned-dose progress and reminders. Edit schedules or archive medications while preserving dose history.
- Improve backup validation, restore previews, recovery backups and transaction rollback. Backups retain daily medication targets and reminder times.
- Preserve logging and medication drafts, make save/error feedback clearer, and improve small-screen and large-text layouts.
- Improve history search, explicit zero-value observations, onboarding choices and cycle-date handling.
- Add optional widget privacy redaction and improve notification navigation.
- Improve CSV/PDF exports, date ranges, temperature scales and long-text wrapping.
- Improve GitHub update checks, cancellation and error feedback.

Database schema changes from 10 to 11; migration tests also cover the path from schema 8. Backup format 3 reads supported earlier backup formats. Android 9/10 app-lock compatibility is outside this work's scope.
