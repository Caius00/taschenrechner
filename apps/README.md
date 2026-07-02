# apps/ – hier kommen die Programme rein

Lege pro Programm **einen Ordner** an. Der Ordnername wird zum App-Namen und zum
Namen des fertigen Pakets (`<Ordnername>.msix`). Es gibt zwei Wege:

## A) Kleine App: Dateien direkt reinlegen
Für kleine, self-contained Programme (jede Datei **unter 100 MB** – GitHub-Limit).

```
apps/MeinProgramm/
├── MeinProgramm.exe        <- Einstiegspunkt (erste .exe oben)
└── ...zugehoerige Dateien
```

Regeln:
- **Self-contained:** alles, was die App braucht (Runtime/DLLs), muss im Ordner sein.
  Die Pipeline fügt **kein** Runtime hinzu.
- **Keine Installer/Setups** – das ist nicht die App, sondern nur deren Installer.
- **Einstiegspunkt** = erste `.exe` auf oberster Ebene (Hilfs-exen in Unterordner).

## B) Grosse App: nur einen Link reinlegen (`app.url`)
Für grosse Programme (Browser, GUI-Apps mit gebündeltem Runtime, …). Statt der
Binärdateien legst du **nur eine Textdatei `app.url`** rein – die Pipeline lädt und
entpackt zur Build-Zeit. So landet **nichts Grosses im Git** und das 100-MB-Limit
gilt nicht.

```
apps/MeineGrosseApp/
└── app.url
```

Inhalt von `app.url`:
```
url = https://.../programm-portable-x64.zip
# optional, falls mehrere/verschachtelte exen:
# exe = unterordner\Start.exe
```

Unterstützte Download-Typen: **`.zip`**, **`.7z`** (werden entpackt) und eine
einzelne portable **`.exe`** (wird direkt genutzt). **Keine Installer** (`setup.exe`).

## Beispiele in diesem Repo
- `Everything…/`, `SumatraPDF/` → Weg A (kleine Dateien committet)
- `ScreenToGif/app.url` → Weg B (grosse GUI-App per URL)

Danach committen + pushen – die Pipeline (`.github/workflows/package.yml`) erledigt
Manifest, Zertifikat, Signierung und Veröffentlichung automatisch.
