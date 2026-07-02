#!/bin/bash
# Erzeugt icon/calculator.png und icon/calculator.ico aus dem Icon-Zeichencode
# (Rechnerfenster.erzeugeIcon in src/Main.java).
# Aufruf:  tools/generate-icons.sh
set -e
cd "$(dirname "$0")/.."

OUT=$(mktemp -d)
javac -d "$OUT" src/Main.java tools/GenerateIcons.java
java -cp "$OUT" GenerateIcons
echo "Fertig -> icon/calculator.png, icon/calculator.ico"
