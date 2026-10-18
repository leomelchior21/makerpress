"""Fail closed if previous public metadata cannot be read, except a genuine first release."""
import json
import os
import sys
import urllib.error
import urllib.request

repository, version_code = sys.argv[1:]
headers = {'Accept': 'application/vnd.github+json', 'Authorization': 'Bearer ' + os.environ['GH_TOKEN']}
request = urllib.request.Request(f'https://api.github.com/repos/{repository}/releases/latest', headers=headers)
try:
    with urllib.request.urlopen(request) as response:
        latest = json.load(response)
except urllib.error.HTTPError as error:
    if error.code == 404:
        sys.exit(0)
    raise
asset = next((item for item in latest['assets'] if item['name'] == 'latest.json'), None)
if not asset:
    raise SystemExit('Previous release has no latest.json; verify version continuity manually before publishing.')
with urllib.request.urlopen(asset['browser_download_url']) as response:
    previous = json.load(response)
if int(version_code) <= int(previous['versionCode']):
    raise SystemExit('VERSION_CODE must strictly exceed the previous published version.')
print('Version code increases monotonically.')
