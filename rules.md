# Project Context & Memories

## Workflows

### LunarLog Update Workflow
1. **Primary (Automated CI Release):**
   - Bump `versionName`/`versionCode` in `app/build.gradle.kts`.
   - Add the matching `## [x.y.z] - YYYY-MM-DD` section to `CHANGELOG.md`.
   - Merge to `main`: GitHub Actions builds a **signed** release APK and publishes a GitHub Release `vX.Y.Z` using the `CHANGELOG.md` section as the release notes.
2. **Fallback (Manual Release):**
   - If CI signing secrets are not configured, follow the manual flow: sign APK locally, draft release, upload APK, publish.

## Project Structure
- **Repository:** `Robertg761/LunarLog` (Public)
- **Updates:** App checks GitHub Releases directly and downloads updates via `DownloadManager` (APK install prompt via `FileProvider`).
