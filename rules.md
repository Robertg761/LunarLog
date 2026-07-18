# Project Context & Memories

## Workflows

### LunarLog Update Workflow
1. **Primary (Automated CI Release):**
   - Bump `versionName`/`versionCode` in `app/build.gradle.kts`.
   - Add the matching `## [x.y.z] - YYYY-MM-DD` section to `CHANGELOG.md`.
   - Push or merge the bump to `main`: GitHub Actions detects the higher version, builds a **signed** release APK, creates tag `vX.Y.Z`, and publishes the GitHub Release using the matching `CHANGELOG.md` section as release notes.
   - A normal `main` push with no `versionName` increase does not publish a release. Manual SemVer tags on the current `main` HEAD remain supported.
2. **Fallback (Manual Release):**
   - If CI signing secrets are not configured, follow the manual flow: sign APK locally, draft release, upload APK, publish.

## Project Structure
- **Repository:** `Robertg761/LunarLog` (Public)
- **Updates:** App checks GitHub Releases directly and downloads updates via `DownloadManager` (APK install prompt via `FileProvider`).
