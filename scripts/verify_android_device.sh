#!/usr/bin/env bash
# Run only on a disposable emulator: these display settings intentionally change it.
set -euo pipefail
profile="${1:-standard}"
case "$profile" in
  standard|compact-large-text) ;;
  *) echo "Unknown display profile: $profile" >&2; exit 2 ;;
esac
report_dir="app/build/device-verification"
mkdir -p "$report_dir"
collect_evidence() {
  result=$?
  trap - EXIT
  adb logcat -d > "$report_dir/logcat.txt" 2>&1 || true
  adb shell dumpsys activity activities > "$report_dir/activities.txt" 2>&1 || true
  adb pull /sdcard/Android/data/com.lunarlog.debug/files/ "$report_dir/files" > "$report_dir/pull.txt" 2>&1 || true
  python3 scripts/report_android_results.py || true
  exit "$result"
}
trap collect_evidence EXIT
adb wait-for-device
adb logcat -c
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
if [[ "$profile" == compact-large-text ]]; then
  adb shell wm size 640x1200
  adb shell wm density 320
  adb shell settings put system font_scale 2.0
else
  adb shell wm size reset
  adb shell wm density reset
  adb shell settings put system font_scale 1.0
fi
adb shell input keyevent 82
{
  adb shell getprop ro.build.version.sdk
  adb shell wm size
  adb shell wm density
  adb shell settings get system font_scale
} > "$report_dir/device.txt"
# Keep test files available until the EXIT trap pulls screenshots and diagnostics.
./gradlew --no-daemon -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true connectedGithubDebugAndroidTest 2>&1 | tee "$report_dir/gradle.txt"
