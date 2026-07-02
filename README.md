# MSIX-Paketierung

Automatische Fabrik: **fertige Windows-Programme rein → signierte MSIX-Pakete raus.**

Du legst deine Programme unter `apps/` ab, pushst, und eine GitHub-Actions-Pipeline
findet jede App, verpackt sie zu einem **signierten MSIX** und legt sie als
**Release** ab – kein manuelles Bauen, Signieren oder Hochladen.

## Bedienung

1. Pro Programm einen Ordner unter `apps/` anlegen – zwei Wege (Details siehe `apps/README.md`):
   - **Kleine App:** Dateien direkt reinlegen (jede Datei < 100 MB)
     ```
     apps/MeinProgramm/MeinProgramm.exe   (+ alle zugehoerigen Dateien)
     ```
   - **Grosse App:** nur einen Link reinlegen – wird zur Build-Zeit geladen
     ```
     apps/MeineGrosseApp/app.url          (Inhalt: url = https://.../app-portable.zip)
     ```
2. Committen und pushen:
   ```bash
   git add apps/
   git commit -m "MeinProgramm hinzugefuegt"
   git push
   ```
3. Die Pipeline läuft automatisch und veröffentlicht die Pakete im Release
   **`latest`** (im Reiter *Releases*): pro App eine `<Name>.msix` und das
   zugehörige `<Name>.cer`.

## Was die Pipeline macht (`.github/workflows/package.yml`)

Für jeden Ordner unter `apps/`:
1. **winapp CLI** per winget installieren.
2. **Manifest** erzeugen (`winapp manifest generate` – Icon wird aus der exe übernommen).
3. **Publisher/Anzeigename** = Ordnername.
4. **Zertifikat** erzeugen (selbstsigniert, passend zum Manifest).
5. **MSIX bauen + signieren** (`winapp package --cert`).
6. **Release** aktualisieren (MSIX + öffentliches Zertifikat).

## Installieren beim Nutzer (Windows)

Da selbstsigniert, muss das Zertifikat einmalig als vertrauenswürdig importiert werden:

```powershell
Import-Certificate -FilePath .\<Name>.cer -CertStoreLocation Cert:\LocalMachine\TrustedPeople
Add-AppxPackage .\<Name>.msix
```

Für eine öffentliche Verteilung ohne diesen Schritt wäre ein Zertifikat einer
vertrauenswürdigen Zertifizierungsstelle (CA) statt des selbstsignierten nötig.
