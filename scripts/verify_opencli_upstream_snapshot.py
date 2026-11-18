"""Replay the locked OpenCLI 1.8.8 manifest, help and browser output fixtures."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "src/test/resources/opencli-contracts/v1"
HELP_CASES = {
    "root.json": (),
    "browser.stdout": ("browser",),
    "browser-tab.json": ("browser", "fixture", "tab"),
    "browser-get.json": ("browser", "fixture", "get"),
    "browser-state.json": ("browser", "fixture", "state"),
    "meta-auth.json": ("auth",),
    "meta-list.txt": ("list",),
    "meta-profile.json": ("profile",),
    "site-zhihu-hot.json": ("zhihu", "hot"),
    "site-npm.json": ("npm",),
    "site-npm-search.json": ("npm", "search"),
}
PAGE_URL = "http://127.0.0.1:18765/basic.html"
BROWSER_CASES = {
    "tab-list.json": (("tab", "list"), {}),
    "tab-list-empty.json": (("tab", "list"), {"OPENCLI_FIXTURE_EMPTY": "1"}),
    "open.json": (("open", PAGE_URL), {}),
    "state-dom.stdout": (("state", "--source", "dom"), {}),
    "state-ax.stdout": (("state", "--source", "ax"), {}),
    "state-compare.json": (("state", "--source", "ax", "--compare-sources"), {}),
    "state-compare-ax-failure.json": (
        ("state", "--compare-sources"), {"OPENCLI_FIXTURE_AX_FAILURE": "1"},
    ),
    "find.json": (("find", "--css", "#fixture", "--limit", "1"), {}),
    "console.json": (("console",), {}),
    "console-level.json": (("console", "--level", "warning"), {"OPENCLI_FIXTURE_CONSOLE_LEVEL": "1"}),
    "network.json": (("network",), {}),
    "analyze.json": (("analyze", PAGE_URL), {}),
}


def verify_manifest(package: Path, lock: dict) -> None:
    metadata = json.loads((package / "package.json").read_bytes())
    if metadata.get("name") != "@jackwener/opencli" or metadata.get("version") != lock["version"]:
        raise SystemExit(f"expected @jackwener/opencli@{lock['version']}")
    baseline = lock["manifest"]
    content = (package / "cli-manifest.json").read_bytes()
    if hashlib.sha256(content).hexdigest() != baseline["sha256"]:
        raise SystemExit("installed cli-manifest.json SHA-256 differs from the lock")
    actual = {f"{entry['site']} {entry['name']}" for entry in json.loads(content)}
    historical = {
        f"{entry['site']} {entry['subcommand']}"
        for entry in json.loads((ROOT / baseline["historicalComparison"]).read_bytes())
    }
    if sorted(actual - historical) != baseline["addedAdapterCommands"]:
        raise SystemExit("added adapter commands differ from the locked comparison")
    if sorted(historical - actual) != baseline["removedAdapterCommands"]:
        raise SystemExit("removed adapter commands differ from the locked comparison")
    print(f"Manifest verified against the lock and historical snapshot ({len(actual)} commands)")


def replay(entry: Path, preload: Path, fixture: Path, argv: tuple[str, ...], settings: dict) -> None:
    with tempfile.TemporaryDirectory(prefix="opencli-fixture-") as isolated:
        state = Path(isolated)
        env = {
            name: value for name, value in os.environ.items()
            if not name.startswith("OPENCLI_") and name != "NODE_OPTIONS"
        }
        env.update({
            "CI": "1",
            "NO_COLOR": "1",
            "OPENCLI_FIXTURE_HOME": isolated,
            "OPENCLI_CONFIG_DIR": str(state / "config"),
            "OPENCLI_CACHE_DIR": str(state / "cache"),
        })
        env.update(settings)
        result = subprocess.run(
            ["node", "--import", str(preload), str(entry), *argv],
            cwd=state, env=env, capture_output=True, timeout=20,
        )
    if result.returncode or result.stderr or result.stdout != fixture.read_bytes():
        raise SystemExit(f"{fixture.name}: replay differs (exit {result.returncode}, "
                         f"stderr {len(result.stderr)} bytes, stdout {len(result.stdout)} bytes)")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("package", type=Path, help="Installed @jackwener/opencli package directory")
    package = parser.parse_args().package.resolve()
    lock = json.loads((FIXTURES / "upstream-1.8.8.lock.json").read_bytes())
    verify_manifest(package, lock)
    entry = package / "dist/src/main.js"
    help_dir = (FIXTURES / lock["structuredHelp"]["checksums"]).parent
    browser_dir = (FIXTURES / lock["browserOutput"]["checksums"]).parent
    with tempfile.TemporaryDirectory(prefix="opencli-help-preload-") as tmp:
        preload = Path(tmp) / "isolate.mjs"
        preload.write_text(
            "import os from 'node:os';\n"
            "import { syncBuiltinESMExports } from 'node:module';\n"
            "os.homedir = () => process.env.OPENCLI_FIXTURE_HOME;\n"
            "syncBuiltinESMExports();\n",
            encoding="utf-8",
        )
        for name, argv in HELP_CASES.items():
            replay(entry, preload, help_dir / name, (*argv, "--help", "-f", "json"), {})
    for name, (argv, settings) in BROWSER_CASES.items():
        replay(entry, browser_dir / "capture.mjs", browser_dir / name, ("browser", "fixture", *argv),
               {"OPENCLI_FIXTURE_FIXED_CLOCK": "1", **settings})
    print(f"Replay matched {len(HELP_CASES)} help and {len(BROWSER_CASES)} browser samples byte for byte")


if __name__ == "__main__":
    main()
