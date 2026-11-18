#!/usr/bin/env bash
# Verify the pinned published CLI in an isolated installation and record live-only evidence.
set -euo pipefail

report_dir=platform-evidence
mkdir -p "$report_dir"
temp_root=${RUNNER_TEMP:?}
if [[ "${RUNNER_OS:-}" == Windows ]]; then
  temp_root=$(cygpath -u "$temp_root")
fi
install_dir=$(mktemp -d "$temp_root/opencli-installed.XXXXXX")
npm install --prefix "$install_dir" --ignore-scripts --no-audit --no-fund @jackwener/opencli@1.8.8

cli_entry="$install_dir/node_modules/@jackwener/opencli/dist/src/main.js"
export CI=1 NO_COLOR=1
node --version > "$report_dir/installed-node.txt"
node "$cli_entry" --version > "$report_dir/installed-opencli-version.txt"
grep -Fx '1.8.8' "$report_dir/installed-opencli-version.txt"

export OPENCLI_INSTALLED_NODE
OPENCLI_INSTALLED_NODE=$(node -p 'process.execPath')
export OPENCLI_INSTALLED_ENTRY="$cli_entry"
tests=OpenCliCapabilityOverrideIT,OpenCliReadOnlyDiagnosticsIT
suites=(
  --suite real-execution=io.github.easy4j.opencli.contract.OpenCliCapabilityOverrideIT
  --suite real-execution=io.github.easy4j.opencli.contract.OpenCliReadOnlyDiagnosticsIT
)
if [[ "${RUNNER_OS:-}" == Windows ]]; then
  registry="$install_dir/registry.json"
  node "$cli_entry" list -f json > "$registry"
  OPENCLI_INSTALLED_ENTRY=$(cygpath -w "$cli_entry")
  export OPENCLI_INSTALLED_REGISTRY
  OPENCLI_INSTALLED_REGISTRY=$(cygpath -w "$registry")
  export OPENCLI_INSTALLED_BATCH_SHIM
  OPENCLI_INSTALLED_BATCH_SHIM=$(cygpath -w "$install_dir/node_modules/.bin/opencli.cmd")
  tests+=,OpenCliInstalledLauncherIT
  suites+=(--suite real-execution=io.github.easy4j.opencli.contract.OpenCliInstalledLauncherIT)
fi

java -version > "$report_dir/installed-java.txt" 2>&1
"$@" -version > "$report_dir/installed-maven.txt" 2>&1
result=0
"$@" -B --no-transfer-progress "-Dtest=$tests" test > "$report_dir/installed-maven.log" 2>&1 || result=$?
tail -80 "$report_dir/installed-maven.log"
python scripts/contract_report.py \
  --reports target/surefire-reports \
  --head "$(git rev-parse HEAD)" \
  --branch "${GITHUB_REF_NAME:-$(git branch --show-current)}" \
  --java-version-file "$report_dir/installed-java.txt" \
  --maven-version-file "$report_dir/installed-maven.txt" \
  --exit-code "$result" \
  "${suites[@]}" \
  --output "$report_dir/installed-report.json"
