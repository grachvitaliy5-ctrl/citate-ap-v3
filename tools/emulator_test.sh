#!/bin/bash
# Проверка APK в эмуляторе: скриншоты вкладок и журнал. Запускается из GitHub Actions.
set -x
OUT=results; mkdir -p $OUT
PKG=com.example.goals
adb install -r apk/app-debug.apk > $OUT/install.txt 2>&1
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb logcat -c
adb shell am start -W -n $PKG/.MainActivity
sleep 30
adb exec-out screencap -p > $OUT/01_open.png
SZ=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1); W=${SZ%x*}; H=${SZ#*x}; echo "$W $H" > $OUT/size.txt
adb shell input tap $((W/2)) $((H/2))
sleep 30
adb exec-out screencap -p > $OUT/02_entered.png
sleep 20
adb exec-out screencap -p > $OUT/03_ready.png
# вкладки внизу: 5 штук
TY=$((H - H/30 - 60))
for i in 1 2 3 4; do
  adb shell input tap $(( (2*i+1)*W/10 )) $TY
  sleep 6
  adb exec-out screencap -p > $OUT/1${i}_tab.png
done
adb shell input tap $((W/10)) $TY
sleep 8
adb exec-out screencap -p > $OUT/20_back_to_path.png
adb logcat -d > $OUT/logcat_full.txt
grep -E "PathJS|PATH-|chromium|AndroidRuntime|FATAL|cr_|WebView|$PKG" $OUT/logcat_full.txt > $OUT/logcat.txt || true
exit 0
