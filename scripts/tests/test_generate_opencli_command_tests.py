import hashlib
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


class ManifestCoverageProvenanceTest(unittest.TestCase):
    def test_explicit_manifest_records_source_and_bytes_without_local_paths(self):
        generator = Path(__file__).resolve().parents[1] / "generate_opencli_command_tests.py"
        source = "https://registry.npmjs.org/@jackwener/opencli/-/opencli-1.8.8.tgz"
        acquisition = "npm pack @jackwener/opencli@1.8.8"
        payload = b'[{"site": "example", "name": "list"}]\n'
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            script = root / "scripts" / generator.name
            script.parent.mkdir()
            script.write_bytes(generator.read_bytes())
            manifest = root / "source checkout" / "cli-manifest.json"
            manifest.parent.mkdir()
            manifest.write_bytes(payload)

            result = subprocess.run(
                [sys.executable, str(script), "--manifest-source", source,
                 "--observed-at", "2026-11-19", "--acquisition-command", acquisition],
                env={**os.environ, "OPENCLI_MANIFEST": str(manifest)},
                capture_output=True, text=True,
            )

            self.assertEqual(0, result.returncode, result.stderr)
            metadata = (root / "src/test/resources/opencli/manifest-coverage-meta.json").read_text()
            self.assertNotIn(temporary, metadata)
            self.assertEqual(
                {
                    "manifestPath": source,
                    "manifestSha256": hashlib.sha256(payload).hexdigest(),
                    "observedAt": "2026-11-19",
                    "acquisitionCommand": acquisition,
                    "scope": "Bundled adapter command enumeration from cli-manifest.json",
                    "manifestCommandCount": 1,
                    "siteCount": 1,
                },
                json.loads(metadata),
            )
