# Project Context & Memories

## Workflows

### LunarLog Update Workflow
1. **Agent Tasks:**
   - Update code/files and bump version in `app/build.gradle.kts`.
   - Update `CHANGELOG.md` with new version details.
   - Commit and push changes.
   - Extract specific text for the new version from `CHANGELOG.md`.
   - Draft GitHub Release (vX.X.X) using the extracted text as the description (do NOT just say "see changelog").
2. **User Tasks:**
   - Manually sign the APK.
   - Manually upload the signed APK to the draft release.
   - Publish the release.

## Project Structure
- **Repository:** `Robertg761/LunarLog` (Public)
- **Updates:** App checks GitHub Releases directly via `AppUpdater` library.
