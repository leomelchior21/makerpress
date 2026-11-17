# Verification report: MakerPress 1.2.0

- JVM/Robolectric: 45 passing tests, no failures or skipped tests.
- Android lint: no errors, 25 warnings (see HTML report).
- Debug APK: 11206033 bytes, package com.makerpress.app, versionCode 3.
- SHA-256: `6cfa56eae62417222a1aaf82c0dfedc8193ac6d83ba9f42c444b8b82f35f7e2f`.

| Suite | Tests | Failures |
| --- | ---: | ---: |
| ActivitiesTest | 6 | 0 |
| ActivityRenderTest | 2 | 0 |
| AndroidPipelineTest | 7 | 0 |
| ClassroomPuzzlesTest | 5 | 0 |
| ComposeLaunchTest | 5 | 0 |
| GeneratorsTest | 5 | 0 |
| PrintingTest | 6 | 0 |
| UpdateDownloadTest | 5 | 0 |
| UpdatesTest | 4 | 0 |

New coverage in 1.2.0:
- Golf holes are playable with a calibrated par across 360 boards (3 difficulties × 40 seeds × 3 lengths), and longer holes have a higher average par.
- Galaxy crystals and portal are reachable, and dungeon keys are reachable before the locked door while the stairs stay behind it (450 boards each).
- Mazes are perfect spanning trees with a valid solution path.
- Every nonogram picture is proven unique by the line solver.
- Bingo cards follow their themes and are unique.
- Share codes round-trip, normalize typing errors and reject bad input. Daily seeds are stable.
- Course par sums are correct, and new blocks serialize.
- Legacy v1 boards still decode and render.
- Every new block renders pure black/white at 384 and 576 dots and at all three texture tones.
- Opening a share code in the UI rebuilds the same maze. Class mode queues 10 distinct numbered sheets plus a combined key.

Printable samples are generated in `app/build/activity-samples/`, and a selection is in `docs/previews/`. UI tests use native-graphics Robolectric, not a physical handset. No connected phone or printer was available. Physical KP-1025 printing (especially texture density), interactive installation and real-device update preservation still need testing. This is the existing local debug/test signing channel, not a production-key release.

Reproduce with `./gradlew.bat testDebugUnitTest lintDebug assembleDebug` (JDK 17, Android SDK 35).
