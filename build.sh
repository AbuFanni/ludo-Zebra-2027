#!/usr/bin/env bash
# בונה את Ludo-Zebra.jar ומריץ את בדיקות המודל.
# הקוד מתקמפל ל-Java 17 (NFR-05), ולכן ה-JAR רץ על כל מחשב עם Java 17 ומעלה.
# צריך JDK 17 ומעלה. אם javac לא נמצא ב-PATH, מגדירים JAVA_HOME לתיקיית ה-JDK.
# עובד ב-macOS, ב-Linux וב-Git Bash ב-Windows. ב-Windows בלי Git Bash: build.bat.
set -euo pipefail
cd "$(dirname "$0")"

BIN="${JAVA_HOME:+$JAVA_HOME/bin/}"

# java ב-Windows מפריד classpath ב-; ובשאר המערכות ב-:
SEP=":"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
esac

rm -rf build
mkdir -p build/classes build/test

"${BIN}javac" --release 17 -encoding UTF-8 -d build/classes $(find src -name '*.java')
cp -r src/View/images build/classes/View/

"${BIN}javac" --release 17 -encoding UTF-8 -cp build/classes -d build/test $(find test -name '*.java')
"${BIN}java" -cp "build/classes${SEP}build/test" Model.ModelTests

printf 'Main-Class: Controller.Main\n' > build/manifest.txt
"${BIN}jar" cfm Ludo-Zebra.jar build/manifest.txt -C build/classes .
echo "Built Ludo-Zebra.jar"
