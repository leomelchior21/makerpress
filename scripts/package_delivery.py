"""Package current build metadata and passing reports; never invent verification results."""
from pathlib import Path
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parent.parent
suites = [ET.parse(p).getroot() for p in (root / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')]
assert suites and all(int(s.attrib[k]) == 0 for s in suites for k in ('failures', 'errors', 'skipped')), 'Run all tests successfully first.'
metadata = json.loads((root / 'app/build/outputs/apk/debug/output-metadata.json').read_text())
element = metadata['elements'][0]
version, code = element['versionName'], element['versionCode']
apk = root / 'app/build/outputs/apk/debug' / element['outputFile']
lint = ET.parse(root / 'app/build/reports/lint-results-debug.xml').getroot()
assert not any(i.attrib.get('severity') in ('Error', 'Fatal') for i in lint.findall('issue')), 'Lint errors remain.'
warnings = sum(i.attrib.get('severity') == 'Warning' for i in lint.findall('issue'))
dist = root / 'dist'
dist.mkdir(exist_ok=True)
target = dist / f'MakerPress-v{version}-debug.apk'
shutil.copy2(apk, target)
with target.open('rb') as stream:
    digest = hashlib.file_digest(stream, 'sha256').hexdigest()
target.with_suffix('.apk.sha256').write_text(f'{digest}  {target.name}\n', encoding='ascii')
tests = sum(int(s.attrib['tests']) for s in suites)
table = '\n'.join(f"| {s.attrib['name'].split('.')[-1]} | {s.attrib['tests']} | 0 |" for s in suites)
report = f"""# Verification report: MakerPress {version}

- JVM/Robolectric: {tests} passing tests, no failures or skipped tests.
- Android lint: no errors, {warnings} warnings (see HTML report).
- Debug APK: {target.stat().st_size} bytes, package {metadata['applicationId']}, versionCode {code}.
- SHA-256: `{digest}`.

| Suite | Tests | Failures |
| --- | ---: | ---: |
{table}

Coverage includes procedural reachability (1,200 game boards), unique number-bond equations, dot coordinates and deterministic generation, graph equations, bilingual UI, saved game copies/reprints, serialization, monochrome rendering, legacy puzzles, database migration, ESC/POS output and APK validation.

Printable samples are generated in `app/build/activity-samples/`. UI tests use native-graphics Robolectric, not a physical handset. No connected phone or printer was available. Physical KP-1025 printing, interactive Android installation and real-device update preservation still require testing. This is the existing local debug/test signing channel, not a production-key release.

Reproduce with `./gradlew.bat testDebugUnitTest lintDebug assembleDebug` (JDK 17, Android SDK 35). The full reports are in `app/build/reports/` and packaged under `dist/reports/`.
"""
(root / 'docs/TEST_REPORT.md').write_text(report, encoding='utf-8')
shutil.copytree(root / 'app/build/reports/tests/testDebugUnitTest', dist / 'reports/tests', dirs_exist_ok=True)
shutil.copy2(root / 'app/build/reports/lint-results-debug.html', dist / 'reports/lint-results-debug.html')
for name in ['INSTALLATION.md', 'PRINTER_TESTING.md', 'RELEASES.md', 'TEST_REPORT.md', 'PAPER_ACTIVITIES.md']:
    shutil.copy2(root / 'docs' / name, dist / name)
info = {'applicationId': metadata['applicationId'], 'versionName': version, 'versionCode': code, 'buildType': 'debug', 'apk': target.name, 'sizeBytes': target.stat().st_size, 'sha256': digest, 'testsPassed': tests, 'physicalPrinterVerified': False}
(dist / 'BUILD_INFO.json').write_text(json.dumps(info, indent=2), encoding='utf-8')
excluded = {'.tools', '.gradle', '.kotlin', '.git', '.idea', 'build', 'dist', '__pycache__'}
with zipfile.ZipFile(dist / f'MakerPress-v{version}-source.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    for path in root.rglob('*'):
        relative = path.relative_to(root)
        if path.is_file() and not any(part in excluded for part in relative.parts) and path.name != 'local.properties' and path.suffix not in {'.jks', '.keystore'}:
            archive.write(path, str(Path('MakerPress') / relative))
print(json.dumps(info, indent=2))
