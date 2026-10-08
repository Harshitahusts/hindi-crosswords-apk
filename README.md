# हिंदी वर्ग पहेली

A simple, fully offline Hindi crossword app for Android: 100 newspaper-style puzzles, a built-in Hindi keyboard, and automatic saving of progress.

- No internet permission, ads, analytics, login or dependencies (plain Android Views + Kotlin).
- Each box holds one akshara (syllable), as in Hindi newspapers: जयपुर = ज | य | पु | र.
- Keyboard rules: a letter fills the box, a matra or ं joins it (क + ि = कि), and after ् the next letter joins too (क + ् + र = क्र).
- All levels are open (`ProgressManager.SEQUENTIAL_UNLOCK = true` switches to unlocking one level at a time).
- "अक्षर दिखाएँ" reveals the selected box when you are stuck.

## Getting the APK
Every push runs **Actions → Build APK**. Open the latest run and download the artifact `hindi-varg-paheli-apk`. It contains `app-debug.apk` and `app-release.apk`.

To build locally in Android Studio, run `./gradlew assembleRelease`.

## Puzzles
- `tools/words.txt` holds the answers and clues (`उत्तर=संकेत`). Edit or add lines there.
- `python3 tools/gen_puzzles.py` regenerates `app/src/main/assets/puzzles.json` (8×8 grids, 8–12 words each).
- `python3 tools/gen_puzzles.py --check` validates all levels. The unit test `PuzzlesTest` types every answer using the in-app keyboard.

## Signing
`app/release.jks` (password `vargpaheli`) is a self-made key for personal sideloading. Replace it before any Play Store release.
