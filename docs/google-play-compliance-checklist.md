# Google Play Compliance Checklist

Last reviewed: April 3, 2026

This checklist is for LunarLog's Google Play submission. Items are checked only when they can be verified from the repository, live public assets, or completed work in this thread. Play Console choices that cannot be read from the repo stay unchecked even if we discussed them.

## Verified Complete

- [x] Public privacy policy URL exists and is live: [https://robertg761.github.io/LunarLog/](https://robertg761.github.io/LunarLog/)
- [x] Privacy policy source is stored in the repo at [docs/privacy-policy.md](/Users/robert/Documents/Projects/LunarLog/docs/privacy-policy.md)
- [x] Privacy policy is publicly accessible, non-geofenced, and HTML-based rather than a PDF
- [x] Privacy policy names the app and describes data handling, sharing, retention, and contact method
- [x] App does not require account creation, login, password, membership, subscription, or reviewer-provided credentials to access core functionality
- [x] App stores core cycle and wellness data locally on-device using Room/DataStore
- [x] Android automatic cloud backup is disabled via `android:allowBackup="false"`
- [x] Play build disables the GitHub updater flow (`ENABLE_GITHUB_UPDATES = false` for the `play` flavor)
- [x] App does not include ad SDKs in Gradle dependencies
- [x] App does not include analytics or crash-reporting SDKs in Gradle dependencies
- [x] App does not include billing / in-app purchase SDKs in Gradle dependencies
- [x] App does not request location permissions
- [x] App does not include user-to-user chat, voice, image, or audio exchange features
- [x] App does not appear to sell or promote age-restricted products or activities

## Still To Do

- [ ] Complete the Health apps declaration in Play Console and select the applicable health feature category
  - Expected category for LunarLog: `Period tracking`
- [x] Add a privacy policy link or privacy policy text inside the app itself
  - Verified in the Settings screen `About` section
- [ ] Complete the Data safety form in Play Console so it matches the app's actual behavior
- [ ] Enter the live privacy policy URL in the designated Play Console privacy policy field
  - URL to use: [https://robertg761.github.io/LunarLog/](https://robertg761.github.io/LunarLog/)
- [ ] Make sure the Play Console content-rating answers match the app package and store listing
- [ ] Make sure the store listing copy, screenshots, and promotional assets remain appropriate for the selected age groups `13-15`, `16-17`, and `18 and over`
- [ ] Reconfirm that you want to target `13-15` and `16-17`
  - If those age groups stay selected, Google may treat some users as children depending on locale and policy context
- [ ] If any part of the app is not appropriate for younger users in the selected audience, either:
  - add a neutral age screen and gate those features for children or users of unknown age
  - or change the target audience selection in Play Console

## Verified N/A For Current Build

- [x] Families ads SDK compliance work is not currently needed because the app does not appear to serve ads
- [x] IAP / subscriptions compliance work is not currently needed because the app does not appear to sell digital goods
- [x] App access credentials are not needed because the app is accessible without login
- [x] Location-sharing disclosures are not needed because the app does not share precise location with other users

## Evidence In Repo

- Manifest permissions and backup setting: [app/src/main/AndroidManifest.xml](/Users/robert/Documents/Projects/LunarLog/app/src/main/AndroidManifest.xml)
- Play vs GitHub flavor behavior: [app/build.gradle.kts](/Users/robert/Documents/Projects/LunarLog/app/build.gradle.kts)
- Dependencies list: [gradle/libs.versions.toml](/Users/robert/Documents/Projects/LunarLog/gradle/libs.versions.toml)
- Local backup/export behavior: [app/src/main/java/com/lunarlog/data/DataManagementRepository.kt](/Users/robert/Documents/Projects/LunarLog/app/src/main/java/com/lunarlog/data/DataManagementRepository.kt)
- Local settings storage: [app/src/main/java/com/lunarlog/data/UserPreferencesRepository.kt](/Users/robert/Documents/Projects/LunarLog/app/src/main/java/com/lunarlog/data/UserPreferencesRepository.kt)
- GitHub updater implementation for non-Play builds: [app/src/main/java/com/lunarlog/update/UpdateRepository.kt](/Users/robert/Documents/Projects/LunarLog/app/src/main/java/com/lunarlog/update/UpdateRepository.kt)
- Main activity gating updater on build flavor: [app/src/main/java/com/lunarlog/MainActivity.kt](/Users/robert/Documents/Projects/LunarLog/app/src/main/java/com/lunarlog/MainActivity.kt)
- Public privacy policy page: [privacy-policy-site/index.html](/Users/robert/Documents/Projects/LunarLog/privacy-policy-site/index.html)

## Policy References

- Google Play Health apps declaration:
  - [Provide information for the health apps declaration form](https://support.google.com/googleplay/android-developer/answer/14738291?hl=en-GB)
- Google Play Health Content and Services:
  - [Health Content and Services](https://support.google.com/googleplay/android-developer/answer/12261419?hl=en)
- Google Play User data / privacy policy requirements:
  - [User data](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en-AU)
- Google Play target audience and content:
  - [Manage target audience and app content settings](https://support.google.com/googleplay/android-developer/answer/9867159?hl=en)
- Google Play Families policy guidance:
  - [Google Play Families policies](https://support.google.com/googleplay/android-developer/answer/9893335/designing-apps-for-children-and-families?hl=en-GB)
- Families ads SDK policy, only relevant if ads are added later:
  - [Families Self-Certified Ads SDK Program](https://support.google.com/googleplay/android-developer/answer/12918983?hl=en)

## Notes

- This is a submission checklist, not legal advice.
- The riskiest currently open item is the missing in-app privacy policy link or text. Google Play's health and privacy policies explicitly require that the privacy policy be available in Play Console and within the app itself.
- Keeping `13-15` and `16-17` selected increases policy surface area even without ads or login. The app is in a much safer policy position than an ad-supported social app, but audience selection still needs deliberate review.
