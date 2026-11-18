"""Run the Java Browser Bridge contract against an isolated real browser."""
import argparse
from hashlib import sha256
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import threading
import time
from urllib.error import URLError
from urllib.parse import quote
from urllib.request import Request, urlopen
from zipfile import ZipFile

import contract_report


OPENCLI_VERSION = '1.8.8'
EXTENSION_VERSION = '1.0.24'
EXTENSION_SHA = 'bad9163f32a66224404e302f52f35391672341d2bd0bf30c8bbaae3c6e6e246c'
CHROME_VERSION = '158.0.8127.18'
DAEMON_STATUS = 'http://127.0.0.1:19825/status'
SUITE = 'io.github.easy4j.opencli.contract.OpenCliBrowserBridgeIT'
PAGE = b"""<!doctype html><title>Bridge fixture</title><main><button id="go">Continue</button></main>
<script>
const capture = setInterval(() => fetch('/api/items'), 250);
setTimeout(() => clearInterval(capture), 120000);
</script>"""


class FixtureHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/api/items':
            body, content_type = b'{"items":[{"id":17}]}', 'application/json'
        else:
            body, content_type = PAGE, 'text/html; charset=utf-8'
        self.send_response(200)
        self.send_header('Content-Type', content_type)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_):
        pass


def download_zip(url, directory, expected_sha=None):
    archive = directory.with_suffix('.zip')
    with urlopen(url, timeout=60) as response, archive.open('wb') as output:
        shutil.copyfileobj(response, output)
    if expected_sha and sha256(archive.read_bytes()).hexdigest() != expected_sha:
        raise RuntimeError(f'Artifact digest mismatch for {url}')
    with ZipFile(archive) as zipped:
        zipped.extractall(directory)
        if os.name != 'nt':
            for entry in zipped.infolist():
                if entry.external_attr >> 16 & 0o111:
                    (directory / entry.filename).chmod(0o755)


def chrome_executable(root):
    platform = {'linux': 'linux64', 'win32': 'win64'}[sys.platform]
    download_zip(
        f'https://storage.googleapis.com/chrome-for-testing-public/{CHROME_VERSION}/{platform}/chrome-{platform}.zip',
        root / 'chrome',
    )
    return root / 'chrome' / f'chrome-{platform}' / ('chrome.exe' if os.name == 'nt' else 'chrome')


def daemon_status():
    with urlopen(Request(DAEMON_STATUS, headers={'X-OpenCLI': '1'}), timeout=1) as response:
        return json.load(response)


def wait_for_bridge(daemon, browser):
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        if daemon.poll() is not None or browser.poll() is not None:
            raise RuntimeError('The isolated daemon or Chrome exited before the extension connected')
        try:
            status = daemon_status()
        except URLError:
            time.sleep(0.25)
            continue
        if status.get('pid') != daemon.pid:
            raise RuntimeError('The Bridge endpoint belongs to a different daemon')
        if status.get('extensionConnected'):
            if (status.get('daemonVersion'), status.get('extensionVersion')) != (OPENCLI_VERSION, EXTENSION_VERSION):
                raise RuntimeError('The connected Bridge versions differ from the pinned versions')
            return status
        time.sleep(0.25)
    raise RuntimeError('The extension did not connect to the isolated daemon')


def prepare_bound_tab(profile, url):
    # Windows Chrome can open a startup tab before its URL is set. Provision the
    # user tab via the independent DevTools endpoint and wait for navigation.
    deadline = time.monotonic() + 15
    port_file = profile / 'DevToolsActivePort'
    while not port_file.exists() and time.monotonic() < deadline:
        time.sleep(0.1)
    port = int(port_file.read_text(encoding='utf-8').splitlines()[0])
    endpoint = f'http://127.0.0.1:{port}/json'
    with urlopen(Request(endpoint + '/new?' + quote(url, safe=':/'), method='PUT'), timeout=5) as response:
        target = json.load(response)['id']
    while time.monotonic() < deadline:
        with urlopen(endpoint + '/list', timeout=2) as response:
            if any(page['id'] == target and page.get('url') == url for page in json.load(response)):
                with urlopen(endpoint + '/activate/' + target, timeout=2):
                    return
        time.sleep(0.1)
    raise RuntimeError('The original Chrome fixture tab did not finish navigation')


def stop(process):
    if process is None or process.poll() is not None:
        return
    process.terminate()
    try:
        process.wait(timeout=8)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait()


def execute(args, root, evidence):
    extension = root / 'extension'
    download_zip(f'https://github.com/jackwener/OpenCLI/releases/download/v{OPENCLI_VERSION}/'
                 f'opencli-extension-v{EXTENSION_VERSION}.zip', extension, EXTENSION_SHA)
    chrome = args.chrome or chrome_executable(root)
    node = shutil.which('node')
    if not node:
        raise RuntimeError('Node executable is unavailable')
    home = root / 'home'
    home.mkdir()
    env = dict(os.environ, HOME=str(home), USERPROFILE=str(home),
               OPENCLI_CONFIG_DIR=str(root / 'config'), OPENCLI_CACHE_DIR=str(root / 'cache'),
               OPENCLI_INSTALLED_NODE=node, OPENCLI_INSTALLED_ENTRY=str(args.cli_entry))
    for name in ('OPENCLI_DAEMON_PORT', 'OPENCLI_CDP_ENDPOINT', 'OPENCLI_CDP_TARGET'):
        env.pop(name, None)

    # Use the upstream loopback port without changing the published binaries.
    try:
        daemon_status()
    except URLError:
        pass
    else:
        raise RuntimeError('An existing OpenCLI daemon already uses the fixture port')

    reports = Path('target/surefire-reports')
    xml = reports / f'TEST-{SUITE}.xml'
    xml.unlink(missing_ok=True)
    java = subprocess.run(['java', '-version'], check=True, capture_output=True, text=True)
    maven_launcher = shutil.which(args.maven[0])
    if not maven_launcher:
        raise RuntimeError(f'Maven launcher is unavailable: {args.maven[0]}')
    maven_command = [maven_launcher, *args.maven[1:]]
    maven = subprocess.run([*maven_command, '-version'], check=True, capture_output=True, text=True)
    daemon = browser = None
    with ThreadingHTTPServer(('127.0.0.1', 0), FixtureHandler) as server:
        threading.Thread(target=server.serve_forever, daemon=True).start()
        url = f'http://127.0.0.1:{server.server_port}/fixture.html'
        env['OPENCLI_BRIDGE_FIXTURE_URL'] = url
        try:
            with (evidence / 'daemon.log').open('w') as log:
                daemon = subprocess.Popen([node, str(args.cli_entry.with_name('daemon.js'))],
                                          cwd=root, env=env, stdout=log, stderr=subprocess.STDOUT)
            profile = root / 'profile'
            chrome_args = [
                '--headless=new', '--no-first-run', '--no-default-browser-check',
                '--disable-background-networking', '--disable-component-update',
                '--disable-sync', '--remote-debugging-port=0', '--no-proxy-server',
                '--host-resolver-rules=MAP localhost 127.0.0.1',
                '--user-data-dir=' + str(profile),
                '--disable-extensions-except=' + str(extension), '--load-extension=' + str(extension),
            ]
            if sys.platform == 'linux' and os.environ.get('GITHUB_ACTIONS') == 'true':
                # Hosted Ubuntu blocks unprivileged user namespaces for this pinned Chrome.
                chrome_args.append('--no-sandbox')
            if sys.platform == 'darwin':
                chrome_args.append('--use-mock-keychain')
            with (evidence / 'chrome.log').open('w') as log:
                browser = subprocess.Popen([str(chrome), *chrome_args, 'about:blank'],
                                           cwd=root, env=env, stdout=log, stderr=subprocess.STDOUT)
            wait_for_bridge(daemon, browser)
            prepare_bound_tab(profile, url.replace('/fixture', '/bound-fixture'))
            with (evidence / 'maven.log').open('w') as log:
                result = subprocess.run([*maven_command, '-B', '--no-transfer-progress', '-Dtest=OpenCliBrowserBridgeIT', 'test'],
                                        env=env, stdout=log, stderr=subprocess.STDOUT, timeout=180)
            status = wait_for_bridge(daemon, browser)
            (evidence / 'bridge.json').write_text(json.dumps({
                'daemonPid': daemon.pid, 'chromePid': browser.pid, 'chromeVersion': CHROME_VERSION,
                'opencliVersion': status['daemonVersion'], 'extensionVersion': status['extensionVersion'],
            }) + '\n', encoding='utf-8')
        finally:
            stop(browser)
            stop(daemon)
            server.shutdown()

    if xml.exists():
        shutil.copyfile(xml, evidence / xml.name)
    report = contract_report.build_report(
        evidence, head=subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
        branch=os.environ.get('GITHUB_REF_NAME') or subprocess.check_output(
            ['git', 'branch', '--show-current'], text=True).strip(),
        java_version=(java.stdout + java.stderr).strip(), maven_version=(maven.stdout + maven.stderr).strip(),
        exit_code=result.returncode, required_suites={'real-execution': [SUITE]},
    )
    (evidence / 'report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report['layers']['real-execution']))
    return 0 if report['status'] == 'PASS' else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cli-entry', required=True, type=lambda path: Path(path).resolve())
    parser.add_argument('--chrome', type=lambda path: Path(path).resolve())
    parser.add_argument('--evidence', required=True, type=lambda path: Path(path).resolve())
    parser.add_argument('maven', nargs='+')
    args = parser.parse_args()
    args.evidence.mkdir(parents=True, exist_ok=False)
    with tempfile.TemporaryDirectory(prefix='opencli-bridge-', ignore_cleanup_errors=True) as root:
        return execute(args, Path(root), args.evidence)


if __name__ == '__main__':
    sys.exit(main())
