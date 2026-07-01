
# Taschenrechner

Ein einfacher Taschenrechner mit grafischer Oberflaeche (Java Swing).
Kann **plus (+), minus (−), mal (\*)** und **geteilt (/)** rechnen und hat ein eigenes Icon.

## Voraussetzungen

- Java (JDK) 17 oder neuer (`java -version` zum Pruefen)

## JAR aus dem Code bauen

```bash
./build.sh
```

Das kompiliert `src/Main.java` und erzeugt daraus die ausfuehrbare `Taschenrechner.jar`.

Alternativ von Hand:

```bash
javac -d out src/Main.java
echo "Main-Class: Main" > manifest.txt
jar --create --file Taschenrechner.jar --manifest manifest.txt \
    -C out Main.class -C out Rechnerfenster.class
rm manifest.txt
```

## Starten

```bash
java -jar Taschenrechner.jar
```

Oder per Doppelklick auf `Taschenrechner.jar` (macOS: Rechtsklick → Öffnen).

## Projektstruktur

```
src/Main.java     Der komplette Quellcode (GUI, Rechenlogik, Icon)
build.sh          Baut die Taschenrechner.jar aus dem Code
```
