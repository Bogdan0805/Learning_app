#!/usr/bin/env python3
"""Встраивает нативный код (баннеры поверх приложений) в Android-проект, созданный `npx cap add android`."""
import re
import shutil
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
src = root / "native" / "android"
app = root / "android" / "app"
pkg_dir = app / "src" / "main" / "java" / "com" / "bogdan0805" / "learningapp"
manifest = app / "src" / "main" / "AndroidManifest.xml"
gradle = app / "build.gradle"


def fail(msg):
    print("ОШИБКА:", msg)
    sys.exit(1)


if not manifest.exists():
    fail("нет android/ — сначала выполни `npx cap add android`")

# 1. Java-файлы (MainActivity заменяется нашей версией с регистрацией плагина)
pkg_dir.mkdir(parents=True, exist_ok=True)
files = sorted(src.glob("*.java"))
if not files:
    fail("в native/android нет .java файлов")
for f in files:
    shutil.copy(f, pkg_dir / f.name)
    print("скопирован", f.name)

# 2. AndroidManifest.xml
text = manifest.read_text(encoding="utf-8")
if "BitReminderService" not in text:
    perms = """
    <!-- Баннеры поверх других приложений и работа в фоне -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
"""
    comps = """
        <service
            android:name="com.bogdan0805.learningapp.BitReminderService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Напоминание об уроках: показывает баннер поверх других приложений" />
        </service>

        <receiver
            android:name="com.bogdan0805.learningapp.WakeReceiver"
            android:exported="false" />

        <receiver
            android:name="com.bogdan0805.learningapp.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>
"""
    if "</application>" not in text or "</manifest>" not in text:
        fail("неожиданный формат AndroidManifest.xml")
    text = text.replace("</application>", comps + "    </application>", 1)
    text = text.replace("</manifest>", perms + "</manifest>", 1)
    manifest.write_text(text, encoding="utf-8")
    print("AndroidManifest.xml обновлён")
else:
    print("AndroidManifest.xml уже содержит службу")

# 3. версия приложения
g = gradle.read_text(encoding="utf-8")
g2 = re.sub(r"versionCode\s+\d+", "versionCode 4", g, count=1)
g2 = re.sub(r'versionName\s+"[^"]*"', 'versionName "0.2.2"', g2, count=1)
if g2 != g:
    gradle.write_text(g2, encoding="utf-8")
    print("версия: 0.2.2 (код 4)")

# 3b. постоянный ключ подписи (чтобы новые версии ставились поверх старых)
g = gradle.read_text(encoding="utf-8")
if "native/debug.keystore" not in g:
    sign = """
    signingConfigs {
        debug {
            storeFile file("../../native/debug.keystore")
            storePassword "android"
            keyAlias "androiddebugkey"
            keyPassword "android"
        }
    }
"""
    m = re.search(r'namespace\s+"[^"]+"\n', g)
    if not m:
        fail("не нашёл namespace в android/app/build.gradle")
    g = g[:m.end()] + sign + g[m.end():]
    gradle.write_text(g, encoding="utf-8")
    print("подпись: постоянный ключ native/debug.keystore")

# 4. проверки
import xml.dom.minidom
xml.dom.minidom.parseString(manifest.read_text(encoding="utf-8"))
main_java = (pkg_dir / "MainActivity.java").read_text(encoding="utf-8")
if "registerPlugin(BitReminderPlugin.class)" not in main_java:
    fail("MainActivity не зарегистрировал плагин")
print("готово")
