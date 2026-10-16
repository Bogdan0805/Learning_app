#!/bin/sh
# Компиляция нативного Android-кода (native/android/*.java) без Android SDK:
# скачивает android.jar (API 34) и компилирует вместе с заглушками com.getcapacitor.*.
# Ловит синтаксические ошибки и обращения к несуществующим API ещё до сборки в облаке.
# Запуск из корня репозитория:  sh tests/java/compile_native.sh
set -e
OUT=/tmp/bit-native-out
JAR=/tmp/android-34.jar
[ -f "$JAR" ] || curl -sL -o "$JAR" https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar
rm -rf "$OUT" && mkdir -p "$OUT"
javac -Xlint:-options -d "$OUT" -cp "$JAR" $(find tests/java/stubs -name '*.java') native/android/*.java
echo "Компиляция нативного кода: OK"
# Чистая логика расписания (без Android) + её тест:
javac -d "$OUT" -cp "$OUT" tests/java/LogicTest.java
java -Dstdout.encoding=UTF-8 -cp "$OUT" com.bogdan0805.learningapp.LogicTest | tail -2
