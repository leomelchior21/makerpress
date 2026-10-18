"""Generate metadata only from an actual signed build. Called after apksigner verify."""
import hashlib
import json
import pathlib
import sys

apk, repository, version, code = sys.argv[1:]
file = pathlib.Path(apk)
assert file.is_file() and file.stat().st_size > 0
manifest = {
    'versionName': version,
    'versionCode': int(code),
    'minimumSdk': 26,
    'apkUrl': f'https://github.com/{repository}/releases/download/v{version}/{file.name}',
    'sha256': hashlib.file_digest(file.open('rb'), 'sha256').hexdigest(),
    'sizeBytes': file.stat().st_size,
    'changelog': [line.removeprefix('- ').strip() for line in pathlib.Path('docs/CHANGELOG.md').read_text(encoding='utf-8').splitlines() if line.startswith('- ')],
}
file.with_name('latest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
file.with_suffix('.apk.sha256').write_text(f"{manifest['sha256']}  {file.name}\n", encoding='ascii')
print(f'Created manifest for {file.name} ({manifest["sizeBytes"]} bytes)')
