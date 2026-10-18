# Verification report: MakerPress 1.1.0

- JVM/Robolectric: 36 passing tests, no failures or skipped tests.
- Android lint: no errors, 20 warnings (see HTML report).
- Debug APK: 11058577 bytes, package com.makerpress.app, versionCode 2.
- SHA-256: `5978730e683933773ddd4d616749d44accd71cfcd5a8e0ca9be0905920fdafb5`.

| Suite | Tests | Failures |
| --- | ---: | ---: |
| ActivitiesTest | 4 | 0 |
| ActivityRenderTest | 1 | 0 |
| AndroidPipelineTest | 7 | 0 |
| ComposeLaunchTest | 4 | 0 |
| GeneratorsTest | 5 | 0 |
| PrintingTest | 6 | 0 |
| UpdateDownloadTest | 5 | 0 |
| UpdatesTest | 4 | 0 |

Coverage includes procedural reachability (1,200 game boards), unique number-bond equations, dot coordinates and deterministic generation, graph equations, bilingual UI, saved game copies/reprints, serialization, monochrome rendering, legacy puzzles, database migration, ESC/POS output and APK validation.

Printable samples are generated in `app/build/activity-samples/`. UI tests use native-graphics Robolectric, not a physical handset. No connected phone or printer was available. Physical KP-1025 printing, interactive Android installation and real-device update preservation still require testing. This is the existing local debug/test signing channel, not a production-key release.

Reproduce with `./gradlew.bat testDebugUnitTest lintDebug assembleDebug` (JDK 17, Android SDK 35). The full reports are in `app/build/reports/` and packaged under `dist/reports/`.
