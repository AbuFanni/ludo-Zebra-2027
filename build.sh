#!/usr/bin/env bash
# בונה את Ludo-Zebra.jar בלי Maven (Java 17 ומעלה). הרצה: ./build.sh
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out && mkdir -p out/classes
javac --release 17 -encoding UTF-8 -d out/classes $(find src/main/java -name '*.java')
printf 'Main-Class: Controller.Main\n' > out/manifest.txt
jar cfm Ludo-Zebra.jar out/manifest.txt -C out/classes .
echo "נבנה: Ludo-Zebra.jar  (הרצה: java -jar Ludo-Zebra.jar)"
