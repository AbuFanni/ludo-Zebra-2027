#!/usr/bin/env bash
# בונה את Ludo-Zebra.jar ומריץ את בדיקות המודל.
# הקוד מתקמפל ל-Java 8, כך שה-JAR רץ על כל Java מגרסה 8 ומעלה (גם על מחשבים עם Java ישנה).
set -euo pipefail
cd "$(dirname "$0")"

rm -rf build
mkdir -p build/classes build/test

javac --release 8 -Xlint:-options -encoding UTF-8 -d build/classes $(find src -name '*.java')
cp -r src/View/images build/classes/View/

javac --release 8 -Xlint:-options -encoding UTF-8 -cp build/classes -d build/test $(find test -name '*.java')
java -cp build/classes:build/test Model.ModelTests

printf 'Main-Class: Controller.Main\n' > build/manifest.txt
jar cfm Ludo-Zebra.jar build/manifest.txt -C build/classes .
echo "Built Ludo-Zebra.jar"
