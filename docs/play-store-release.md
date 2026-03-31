# Play Store Release Setup

LunarLog now has two release channels:

- `playRelease`: Google Play build without the sideload updater permission.
- `githubRelease`: direct-download APK build with the existing GitHub updater flow.

## Local Signing

Populate a root-level `keystore.properties` file from `keystore.properties.example`, or set the same values as environment variables:

```properties
LL_SIGNING_STORE_FILE=/absolute/path/to/lunarlog-upload-key.jks
LL_SIGNING_STORE_PASSWORD=your-store-password
LL_SIGNING_KEY_ALIAS=your-key-alias
LL_SIGNING_KEY_PASSWORD=your-key-password
LL_SIGNING_STORE_TYPE=JKS
```

Google Play recommends using Play App Signing with a dedicated upload key. If your existing GitHub release key is already in user devices and you want identical signatures everywhere, reuse that same key as the upload key instead of generating a second one.

## Build Commands

Build the Play bundle:

```bash
./scripts/build_play_bundle.sh
```

The helper script stops early if signing is not configured, so it won't produce an unsigned bundle by accident.

Or directly:

```bash
./gradlew :app:bundlePlayRelease
```

Build the GitHub APK:

```bash
./gradlew :app:assembleGithubRelease
```

## Upload Checklist

1. Create the app in Play Console with package name `com.lunarlog`.
2. Enroll in Play App Signing and register your upload key.
3. Upload `app/build/outputs/bundle/playRelease/app-play-release.aab` to the Internal testing track first.
4. Complete the Data safety form. LunarLog handles menstrual-cycle and wellness data, so answer this carefully and keep it aligned with the app’s actual local-first behavior.
5. Add a privacy policy URL before production rollout.
6. Fill in App content declarations, store listing copy, screenshots, icon, and feature graphic.
7. Verify the notification permission explanation and any health-related positioning in the store listing are accurate and non-misleading.

## Notes

- `playRelease` targets API 35, which is required for current Google Play submissions.
- The Play build removes `REQUEST_INSTALL_PACKAGES`, which avoids sending the app through restricted-permission review for its updater flow.
- The existing GitHub Actions release workflow now builds `githubRelease` so the sideload APK path still works.
- If you want existing sideload users to upgrade in place to the Play build, you still need the original release keystore that signed those GitHub APKs. A new upload key alone does not preserve that upgrade path.
