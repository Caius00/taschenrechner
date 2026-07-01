#!/bin/bash
# Baut die ausfuehrbare Taschenrechner.jar neu.
# Aufruf:  ./build.sh
set -e
cd "$(dirname "$0")"

rm -rf out
mkdir -p out
javac -d out src/Main.java

echo "Main-Class: Main" > manifest.txt
jar --create --file Taschenrechner.jar --manifest manifest.txt \
    -C out Main.class -C out Rechnerfenster.class
rm -f manifest.txt

echo "Fertig -> Taschenrechner.jar"
echo "Starten mit:  java -jar Taschenrechner.jar"
