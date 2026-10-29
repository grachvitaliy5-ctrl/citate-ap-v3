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
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml $OUT/ui_path.xml
PID=$(adb shell pidof $PKG | tr -d '\r')
adb forward tcp:9222 localabstract:webview_devtools_remote_$PID
sleep 2
node tools/cdp_probe.js $OUT > $OUT/cdp.txt 2>&1
# бой с главной маской: перенос к воротам с запасом светлячков
node tools/cdp_eval.js "window.__p.addLight(30);window.__p.toGate('Тест',1);1" > $OUT/gate.txt 2>&1
for t in 35 45 55 65 80; do sleep 10; [ $t -ge 45 ] && adb exec-out screencap -p > $OUT/40_duel_$t.png; done
sleep 15
adb exec-out screencap -p > $OUT/41_after_duel.png
node tools/cdp_eval.js "JSON.stringify({p:window.__p.path(),b:window.__p.boss(),s:window.__p.state()})" >> $OUT/gate.txt 2>&1
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
# сравнение: та же сцена в простой Activity без Compose
adb root; sleep 3
# уведомление: вечернее напоминание рутин и проверка звука каналов
adb shell am broadcast -n $PKG/.AlarmReceiver --es kind evening
sleep 6
adb shell dumpsys notification --noredact > $OUT/notifications.txt
adb exec-out screencap -p > $OUT/25_after_notify.png
adb shell am start -n $PKG/.DemoActivity
sleep 25
adb exec-out screencap -p > $OUT/30_demo.png
adb shell input tap $((W/2)) $((H/2))
sleep 25
adb exec-out screencap -p > $OUT/31_demo_entered.png
adb logcat -d > $OUT/logcat_full.txt
grep -E "PathJS|PATH-|chromium|AndroidRuntime|FATAL|cr_|WebView|$PKG" $OUT/logcat_full.txt > $OUT/logcat.txt || true
exit 0
