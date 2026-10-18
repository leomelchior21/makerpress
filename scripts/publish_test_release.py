"""Publish the already verified local test APK. Git credentials stay in memory.

Usage: python scripts/publish_test_release.py dist/latest.json
Run tests, lint, apksigner and release_manifest.py first. Push the source commit first.
This deliberately does not create or upload signing keys.
"""
import hashlib
import json
import pathlib
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

REPOSITORY = "leomelchior21/makerpress"
manifest_path = pathlib.Path(sys.argv[1])
manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
version = manifest["versionName"]
tag = f"v{version}"
apk = manifest_path.parent / pathlib.PurePosixPath(urllib.parse.urlparse(manifest["apkUrl"]).path).name
assert manifest["apkUrl"] == f"https://github.com/{REPOSITORY}/releases/download/{tag}/{apk.name}"
assert apk.stat().st_size == manifest["sizeBytes"]
with apk.open("rb") as stream:
    assert hashlib.file_digest(stream, "sha256").hexdigest() == manifest["sha256"]
checksum = apk.with_suffix(".apk.sha256")
assert checksum.read_text().split()[0] == manifest["sha256"]
assert not subprocess.check_output(["git", "status", "--porcelain"], text=True).strip(), "Commit all source changes first"
commit = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
remote = subprocess.check_output(["git", "ls-remote", "origin", "refs/heads/main"], text=True).split()[0]
assert remote == commit, "Push this commit to main before publishing"
credential = subprocess.run(["git", "credential", "fill"], input="protocol=https\nhost=github.com\n\n", text=True, capture_output=True, check=True, timeout=30)
fields = dict(line.split("=", 1) for line in credential.stdout.splitlines() if "=" in line)
token = fields["password"]

def request(url, data=None, method=None, content_type="application/json"):
    if not url.startswith("https://"):
        raise ValueError("HTTPS required")
    body = json.dumps(data).encode() if isinstance(data, dict) else data
    req = urllib.request.Request(url, data=body, method=method, headers={
        "Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json", "Content-Type": content_type,
    })
    with urllib.request.urlopen(req, timeout=180) as response:
        return json.load(response)

api = f"https://api.github.com/repos/{REPOSITORY}/releases"
try:
    latest = request(api + "/latest")
    prior_asset = next((a for a in latest["assets"] if a["name"] == "latest.json"), None)
    if prior_asset:
        with urllib.request.urlopen(prior_asset["browser_download_url"], timeout=30) as response:
            previous = json.load(response)
        assert manifest["versionCode"] > previous["versionCode"], "Version code must increase"
except urllib.error.HTTPError as error:
    if error.code != 404:
        raise
existing = request(api)
assert not any(r["tag_name"] == tag for r in existing), "Release exists; inspect it rather than overwriting assets"
notes = pathlib.Path("docs/CHANGELOG.md").read_text(encoding="utf-8")
release = request(api, {"tag_name": tag, "target_commitish": commit, "name": f"MakerPress {version} — local test channel",
                        "body": notes, "draft": True, "prerelease": False})
print(f"Draft created: {release['id']}. Uploading APK, checksum and manifest.", flush=True)
for asset in (apk, checksum, manifest_path):
    url = release["upload_url"].split("{")[0] + "?name=" + urllib.parse.quote(asset.name)
    request(url, asset.read_bytes(), "POST", "application/octet-stream")
published = request(api + f"/{release['id']}", {"draft": False, "make_latest": "true"}, "PATCH")
print(published["html_url"])
