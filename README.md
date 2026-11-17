# MakerPress

**Create. Print. Play.** Native Android app for offline creative printing on 58 mm Bluetooth thermal printers.

Fourteen tools in four groups. Paper games: Golf, Galaxy, Dungeon and Maze. Puzzles: Sudoku, Nonogram, Kakooma, Word Search and Connect the Dots. Classroom: Math Challenges and Bingo. Studio: Text, Image and QR. Includes a persistent **PT-BR / EN** switch, exact monochrome previews, saved creations, a print queue, printer profiles and an APK updater.

## New in 1.2.0

- **Golf, redesigned.** Terrain areas melt into rounded shapes, drawn with 1-bit textures made for thermal dots: fairway, green, sand, water and two kinds of trees. A printed key explains each texture and its rule. Par is calculated from the real dice odds. Holes come in short, medium and long lengths.
- **Course mode.** Print 3, 6 or 9 holes (par 3 to 7) with a scorecard for 1 to 4 players.
- **Galaxy, redesigned** with nebulas, asteroid fields, a base and a portal, plus its own key and tracker.
- **New activities.** Dungeon is a dice-and-pencil crawl with monsters, traps, a key and a locked door. Maze generates perfect mazes with optional stars. Nonogram has 19 original pictures, each with exactly one logical solution. Bingo prints numbers 1–75, times-table or word cards with a caller list.
- **Share codes.** Every game and puzzle print shows a code such as `GM0-7K2QX9`. Enter it under **I have a code** to print the same map on any phone.
- **Daily challenge.** Everyone gets the same map each day, with no server involved.
- **Class mode.** Queue one different, numbered sheet per student (10–40), plus a combined answer key or a bingo caller list.
- **Better preview.** Fit-to-screen, actual-size and 2× views, paper length in centimetres, a dice button for a new level and a copyable code.
- **Texture calibration strip.** Print it from Printer Lab, then set **Map textures** to lighter, normal or darker in the print profile.
- All print art shares one drawing kit: textures, merged areas, icons, keys, trackers and scorecards.

Printable samples: [Golf](docs/previews/golf.png), [3-hole course](docs/previews/course.png), [Galaxy](docs/previews/galaxy.png), [Dungeon](docs/previews/dungeon.png), [Maze](docs/previews/maze.png), [Nonogram](docs/previews/nonogram.png), [Bingo](docs/previews/bingo.png), [Calibration strip](docs/previews/calibration.png).

## 1.1.0

- Original procedural Golf and Galaxy paper games inspired by Gladden Design, with complete rules and score tracks.
- A fresh level per queued print, distinct extra copies, and exact reprints from history.
- Geometry, linear functions, quadratic formula/Bhaskara and physics: cars, trains, balls and weights.
- Draw graphs from equations or recover equations from graphs; separate worked answer keys.
- Kakooma-style addition puzzles with unique solutions, and six original connect-the-dots drawings.
- Selected Brazilian secondary-school practice associated with BNCC skills. [Research and generation design](docs/PAPER_ACTIVITIES.md).

Printable samples: [Kakooma-style puzzle](docs/previews/kakooma.png), [Connect the Dots](docs/previews/connect-dots.png).

## Update your app

[Download MakerPress from GitHub Releases](https://github.com/leomelchior21/makerpress/releases).

For the original 1.0.0 installation, go to **Ajustes → Atualizações**, enter this manifest address, then choose **Salvar e verificar atualizações → Baixar atualização → Instalar atualização**:

```text
https://github.com/leomelchior21/makerpress/releases/latest/download/latest.json
```

Android may ask you to allow installation from MakerPress; return and tap Install again afterward. Do not uninstall the old app. This local test distribution uses the same Android debug certificate as the original APK from this computer. Version 1.1.0 and later have the update address built in. No production signing key is configured yet.

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

KP-1025 compatibility and the new textures still need physical testing (print the calibration strip first). Bluetooth Classic/SPP, 384 dots and ESC/POS are configurable assumptions. No backend, login, ads, subscription, WebView or AI service is required to create or print.
