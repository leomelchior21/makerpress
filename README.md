# MakerPress

**Create. Print. Play.** Native Android app for offline creative printing on 58 mm Bluetooth thermal printers.

Ten tools: Text Studio, Image Studio, Sudoku, Word Search, QR Studio, Math Challenges, Golf, Galaxy, Kakooma and Connect the Dots. Includes a persistent **PT-BR / EN** switch, exact monochrome previews, saved creations, a print queue, printer profiles and an APK updater.

## New in 1.1.0

- Original procedural Golf and Galaxy paper games inspired by Gladden Design, with complete rules and score tracks.
- A fresh level per queued print, distinct extra copies, and exact reprints from history.
- Geometry, linear functions, quadratic formula/Bhaskara and physics: cars, trains, balls and weights.
- Draw graphs from equations or recover equations from graphs; separate worked answer keys.
- Kakooma-style addition puzzles with unique solutions, and six original connect-the-dots drawings.
- Selected Brazilian secondary-school practice associated with BNCC skills. [Research and generation design](docs/PAPER_ACTIVITIES.md).

Printable samples: [Golf](docs/previews/golf.png), [Galaxy](docs/previews/galaxy.png), [Kakooma-style puzzle](docs/previews/kakooma.png), [Connect the Dots](docs/previews/connect-dots.png).

## Update your app

[Download MakerPress from GitHub Releases](https://github.com/leomelchior21/makerpress/releases).

For the original 1.0.0 installation, go to **Ajustes → Atualizações**, enter this manifest address, then choose **Salvar e verificar atualizações → Baixar atualização → Instalar atualização**:

```text
https://github.com/leomelchior21/makerpress/releases/latest/download/latest.json
```

Android may ask you to allow installation from MakerPress; return and tap Install again afterward. Do not uninstall the old app. This local test distribution uses the same Android debug certificate as the original APK from this computer. Version 1.1.0 has the update address built in. No production signing key is configured yet.

## Build

Use Android Studio, or JDK 17 and Android SDK 35:

```powershell
./gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

On Linux/macOS use `chmod +x gradlew` and `./gradlew`. Set `ANDROID_HOME` or `sdk.dir` in ignored `local.properties`. The APK is `app/build/outputs/apk/debug/app-debug.apk`. On this workstation `scripts/build-local.ps1` selects the locally installed JDK and copies the verified APK into `dist/`.

## Guides

- [Installation and printing](docs/INSTALLATION.md)
- [Physical printer checks](docs/PRINTER_TESTING.md)
- [Signing, releases and updates](docs/RELEASES.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Test report](docs/TEST_REPORT.md)

KP-1025 compatibility still needs physical testing. Bluetooth Classic/SPP, 384 dots and ESC/POS are configurable assumptions. No backend, login, ads, subscription, WebView or AI service is required to create or print.
