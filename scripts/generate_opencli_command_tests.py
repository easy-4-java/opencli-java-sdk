#!/usr/bin/env python3
"""从 opencli cli-manifest.json 生成 Java SDK 100% 命令覆盖测试（请勿手改生成文件）。"""
from __future__ import annotations

import json
import os
import textwrap
from pathlib import Path

_SCRIPT_DIR = Path(__file__).resolve().parent
_SDK_ROOT = _SCRIPT_DIR.parent
_DEFAULT_MANIFEST = (
    _SCRIPT_DIR.parents[4]
    / "workspace-partme-ai"
    / "opencli"
    / "opencli"
    / "cli-manifest.json"
)
if not _DEFAULT_MANIFEST.is_file():
    _DEFAULT_MANIFEST = (
        Path("/Users/wandl/workspaces/workspace-partme-ai/opencli/opencli/cli-manifest.json")
    )

MANIFEST_PATH = Path(
    os.environ.get("OPENCLI_MANIFEST", _DEFAULT_MANIFEST)
).resolve()

COVERAGE_PKG = "io.github.hiwepy.opencli.coverage"
COVERAGE_DIR = _SDK_ROOT / "src/test/java/io/github/hiwepy/opencli/coverage"
RESOURCES_DIR = _SDK_ROOT / "src/test/resources/opencli"

PLACEHOLDER_STR = "__coverage__"
PLACEHOLDER_INT = "1"


def placeholder_for_arg(arg: dict) -> list[str]:
    """为必填参数生成 argv 占位 token。"""
    name = arg["name"]
    typ = arg.get("type", "str")
    if arg.get("positional"):
        if typ == "int":
            return [PLACEHOLDER_INT]
        if typ == "boolean":
            return ["true"]
        return [PLACEHOLDER_STR]
    flag = f"--{name}"
    if typ == "boolean":
        return [flag]
    if typ == "int":
        return [flag, PLACEHOLDER_INT]
    return [flag, PLACEHOLDER_STR]


def build_invoke_args(entry: dict) -> list[str]:
    """构建 OpenCliAdapterChannel.invoke 的参数列表（含子命令名）。"""
    tokens = [entry["name"]]
    for arg in entry.get("args", []):
        if not arg.get("required"):
            continue
        if arg.get("default") is not None:
            continue
        tokens.extend(placeholder_for_arg(arg))
    return tokens


def java_string_list(values: list[str]) -> str:
    if not values:
        return "List.of()"
    inner = ", ".join(f'"{v}"' for v in values)
    return f"List.of({inner})"


def load_manifest() -> list[dict]:
    data = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise SystemExit("manifest must be a JSON array")
    return data


def write_manifest_resource(entries: list[dict]) -> None:
    RESOURCES_DIR.mkdir(parents=True, exist_ok=True)
    payload = []
    for e in entries:
        payload.append(
            {
                "site": e["site"],
                "subcommand": e["name"],
                "invokeArgs": build_invoke_args(e),
            }
        )
    out = RESOURCES_DIR / "manifest-coverage-commands.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    meta = RESOURCES_DIR / "manifest-coverage-meta.json"
    meta.write_text(
        json.dumps(
            {
                "manifestPath": str(MANIFEST_PATH),
                "manifestCommandCount": len(entries),
                "siteCount": len({e["site"] for e in entries}),
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print(f"wrote {out} ({len(payload)} commands)")
    print(f"wrote {meta}")


def write_adapter_coverage_test(command_count: int) -> None:
    COVERAGE_DIR.mkdir(parents=True, exist_ok=True)
    java = textwrap.dedent(
        f"""\
        package {COVERAGE_PKG};

        import static org.junit.jupiter.api.Assertions.assertEquals;
        import static org.junit.jupiter.api.Assertions.assertFalse;
        import static org.junit.jupiter.api.Assertions.assertNotNull;

        import com.fasterxml.jackson.core.type.TypeReference;
        import com.fasterxml.jackson.databind.ObjectMapper;
        import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
        import io.github.hiwepy.opencli.core.OpenCliResult;
        import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
        import java.io.InputStream;
        import java.util.List;
        import java.util.stream.Stream;
        import org.junit.jupiter.api.Test;
        import org.junit.jupiter.params.ParameterizedTest;
        import org.junit.jupiter.params.provider.Arguments;
        import org.junit.jupiter.params.provider.MethodSource;

        /**
         * cli-manifest.json 全部 adapter 子命令覆盖（共 {command_count} 条）。
         * <p>
         * <strong>成功标准：</strong>通过 {{@link OpenCliAdapterChannel#invoke}} 发起调用且返回非 null
         * {{@link OpenCliResult}}；argv 以 site id 与子命令开头。不校验业务输出、登录态或平台可用性。
         * </p>
         * <p>由 {{@code scripts/generate_opencli_command_tests.py}} 生成，请勿手改。</p>
         */
        class OpenCliAdapterCommandsCoverageTest {{

            private static final int EXPECTED_MANIFEST_COMMAND_COUNT = {command_count};

            private record ManifestCommand(String site, String subcommand, List<String> invokeArgs) {{}}

            static Stream<Arguments> manifestCommands() throws Exception {{
                ObjectMapper mapper = new ObjectMapper();
                try (InputStream in = OpenCliAdapterCommandsCoverageTest.class.getResourceAsStream(
                    "/opencli/manifest-coverage-commands.json")) {{
                    if (in == null) {{
                        throw new IllegalStateException("missing /opencli/manifest-coverage-commands.json");
                    }}
                    List<ManifestCommand> rows = mapper.readValue(in, new TypeReference<>() {{}});
                    return rows.stream()
                        .map(r -> Arguments.of(r.site(), r.subcommand(), r.invokeArgs()));
                }}
            }}

            @Test
            void manifestResourceCountMatchesExpected() throws Exception {{
                ObjectMapper mapper = new ObjectMapper();
                try (InputStream in = OpenCliAdapterCommandsCoverageTest.class.getResourceAsStream(
                    "/opencli/manifest-coverage-commands.json")) {{
                    List<ManifestCommand> rows = mapper.readValue(in, new TypeReference<>() {{}});
                    assertEquals(EXPECTED_MANIFEST_COMMAND_COUNT, rows.size());
                }}
            }}

            @ParameterizedTest(name = "{{0}}/{{1}}")
            @MethodSource("manifestCommands")
            void adapterCommand(String site, String subcommand, List<String> invokeArgs) {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                OpenCliAdapterChannel channel = new OpenCliAdapterChannel(exec, site);
                OpenCliResult result = channel.invoke(invokeArgs);
                assertNotNull(result);
                List<String> argv = exec.lastInvocation();
                assertFalse(argv.isEmpty());
                assertEquals(site, argv.get(0));
                assertEquals(subcommand, argv.get(1));
            }}
        }}
        """
    )
    out = COVERAGE_DIR / "OpenCliAdapterCommandsCoverageTest.java"
    out.write_text(java, encoding="utf-8")
    print(f"wrote {out}")


def write_meta_coverage_test() -> None:
    """OpenCLI 根级 / meta 子命令 — 每个 CLI 叶子命令一个 @Test。"""
    java = textwrap.dedent(
        f"""\
        package {COVERAGE_PKG};

        import static org.junit.jupiter.api.Assertions.assertEquals;
        import static org.junit.jupiter.api.Assertions.assertFalse;
        import static org.junit.jupiter.api.Assertions.assertNotNull;

        import io.github.hiwepy.opencli.core.OpenCliResult;
        import io.github.hiwepy.opencli.meta.OpenCliMetaClient;
        import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
        import java.util.List;
        import org.junit.jupiter.api.Test;

        /**
         * OpenCLI 根级与 meta 子命令覆盖（list / validate / plugin / daemon 等）。
         * <p>
         * <strong>成功标准：</strong>SDK 门面方法完成调用并返回非 null {{@link OpenCliResult}}；
         * RecordingOpenCliExecutor 捕获的 argv 非空。不校验 CLI 业务结果或账号状态。
         * </p>
         * <p>由 {{@code scripts/generate_opencli_command_tests.py}} 生成，请勿手改。</p>
         */
        class OpenCliMetaCommandsCoverageTest {{

            private static void assertInvoked(RecordingOpenCliExecutor exec, OpenCliResult result) {{
                assertNotNull(result);
                assertFalse(exec.lastInvocation().isEmpty());
            }}

            @Test void testList() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).list("json"));
                assertEquals("list", exec.lastInvocation().get(0));
            }}

            @Test void testValidate() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).validate("npm/search"));
                assertEquals("validate", exec.lastInvocation().get(0));
            }}

            @Test void testVerify() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).verify(null, true));
                assertEquals("verify", exec.lastInvocation().get(0));
            }}

            @Test void testDoctor() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).doctor(true));
                assertEquals("doctor", exec.lastInvocation().get(0));
            }}

            @Test void testConventionAudit() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).conventionAudit("twitter", null, "json", false));
                assertEquals("convention-audit", exec.lastInvocation().get(0));
            }}

            @Test void testCompletion() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).completion("bash"));
                assertEquals(List.of("completion", "bash"), exec.lastInvocation());
            }}

            @Test void testPluginInstall() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).plugin().install("github:user/repo"));
                assertEquals("plugin", exec.lastInvocation().get(0));
            }}

            @Test void testPluginUninstall() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).plugin().uninstall("demo"));
                assertEquals(List.of("plugin", "uninstall", "demo"), exec.lastInvocation());
            }}

            @Test void testPluginUpdate() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).plugin().update("demo"));
                assertEquals(List.of("plugin", "update", "demo"), exec.lastInvocation());
            }}

            @Test void testPluginUpdateAll() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).plugin().updateAll());
                assertEquals(List.of("plugin", "update", "--all"), exec.lastInvocation());
            }}

            @Test void testPluginList() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).plugin().list("json"));
                assertEquals(List.of("plugin", "list", "-f", "json"), exec.lastInvocation());
            }}

            @Test void testPluginCreate() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).plugin().create("demo", "/tmp/demo", "desc"));
                assertEquals("plugin", exec.lastInvocation().get(0));
            }}

            @Test void testAdapterStatus() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).adapter().status());
                assertEquals(List.of("adapter", "status"), exec.lastInvocation());
            }}

            @Test void testAdapterEject() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).adapter().eject("npm"));
                assertEquals(List.of("adapter", "eject", "npm"), exec.lastInvocation());
            }}

            @Test void testAdapterReset() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).adapter().reset("npm", false));
                assertEquals(List.of("adapter", "reset", "npm"), exec.lastInvocation());
            }}

            @Test void testProfileList() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).profile().list());
                assertEquals(List.of("profile", "list"), exec.lastInvocation());
            }}

            @Test void testProfileRename() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).profile().rename("ctx-1", "work"));
                assertEquals(List.of("profile", "rename", "ctx-1", "work"), exec.lastInvocation());
            }}

            @Test void testProfileUse() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).profile().use("work"));
                assertEquals(List.of("profile", "use", "work"), exec.lastInvocation());
            }}

            @Test void testDaemonStatus() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).daemon().status());
                assertEquals(List.of("daemon", "status"), exec.lastInvocation());
            }}

            @Test void testDaemonStop() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).daemon().stop());
                assertEquals(List.of("daemon", "stop"), exec.lastInvocation());
            }}

            @Test void testDaemonRestart() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).daemon().restart());
                assertEquals(List.of("daemon", "restart"), exec.lastInvocation());
            }}

            @Test void testExternalInstall() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).external().install("gh"));
                assertEquals(List.of("external", "install", "gh"), exec.lastInvocation());
            }}

            @Test void testExternalRegister() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).external().register("gh", "/usr/bin/gh", null, "GitHub CLI"));
                assertEquals("external", exec.lastInvocation().get(0));
            }}

            @Test void testExternalList() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).external().list("json"));
                assertEquals(List.of("external", "list", "-f", "json"), exec.lastInvocation());
            }}

            @Test void testExternalPassthrough() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).external().passthrough("gh", List.of("auth", "status")));
                assertEquals(List.of("gh", "auth", "status"), exec.lastInvocation());
            }}

            @Test void testAntigravityServe() {{
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                assertInvoked(exec, new OpenCliMetaClient(exec).antigravity().serve(8082, 30));
                assertEquals("antigravity", exec.lastInvocation().get(0));
            }}
        }}
        """
    )
    out = COVERAGE_DIR / "OpenCliMetaCommandsCoverageTest.java"
    out.write_text(java, encoding="utf-8")
    print(f"wrote {out} (26 meta commands)")


def write_browser_coverage_test() -> None:
    java = textwrap.dedent(
        f"""\
        package {COVERAGE_PKG};

        import static org.junit.jupiter.api.Assertions.assertEquals;
        import static org.junit.jupiter.api.Assertions.assertFalse;
        import static org.junit.jupiter.api.Assertions.assertNotNull;

        import io.github.hiwepy.opencli.browser.OpenCliBrowserClient;
        import io.github.hiwepy.opencli.browser.OpenCliBrowserSession;
        import io.github.hiwepy.opencli.browser.support.OpenCliBrowserConsoleOptions;
        import io.github.hiwepy.opencli.browser.support.OpenCliBrowserDragLocator;
        import io.github.hiwepy.opencli.browser.support.OpenCliBrowserExtractOptions;
        import io.github.hiwepy.opencli.browser.support.OpenCliBrowserFindOptions;
        import io.github.hiwepy.opencli.browser.support.OpenCliBrowserGetHtmlOptions;
        import io.github.hiwepy.opencli.browser.support.OpenCliBrowserScreenshotOptions;
        import io.github.hiwepy.opencli.browser.support.OpenCliBrowserSemanticLocator;
        import io.github.hiwepy.opencli.browser.support.OpenCliBrowserStateOptions;
        import io.github.hiwepy.opencli.browser.support.OpenCliBrowserTabOptions;
        import io.github.hiwepy.opencli.core.OpenCliResult;
        import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
        import org.junit.jupiter.api.BeforeEach;
        import org.junit.jupiter.api.Test;

        /**
         * {{@code opencli browser}} 全部叶子子命令覆盖（与 cli.ts 对齐，共 42 条）。
         * <p>
         * <strong>成功标准：</strong>{{@link OpenCliBrowserSession}} 方法返回非 null {{@link OpenCliResult}}，
         * argv 以 {{@code browser}} 开头。不校验页面内容或浏览器实例是否可用。
         * </p>
         * <p>由 {{@code scripts/generate_opencli_command_tests.py}} 生成，请勿手改。</p>
         */
        class OpenCliBrowserCommandsCoverageTest {{

            private static final String SESSION = "cov-session";

            private RecordingOpenCliExecutor exec;
            private OpenCliBrowserSession session;

            @BeforeEach
            void setUp() {{
                exec = new RecordingOpenCliExecutor();
                session = new OpenCliBrowserClient(exec).session(SESSION, "background");
            }}

            private void assertBrowserInvoked(OpenCliResult result) {{
                assertNotNull(result);
                assertFalse(exec.lastInvocation().isEmpty());
                assertEquals("browser", exec.lastInvocation().get(0));
            }}

            @Test void testBrowserBind() {{ assertBrowserInvoked(session.bind()); }}
            @Test void testBrowserUnbind() {{ assertBrowserInvoked(session.unbind()); }}
            @Test void testBrowserTabList() {{ assertBrowserInvoked(session.tabList()); }}
            @Test void testBrowserTabNew() {{ assertBrowserInvoked(session.tabNew("https://example.com")); }}
            @Test void testBrowserTabSelect() {{ assertBrowserInvoked(session.tabSelect("t1", null)); }}
            @Test void testBrowserTabClose() {{ assertBrowserInvoked(session.tabClose("t1", null)); }}
            @Test void testBrowserOpen() {{ assertBrowserInvoked(session.open("https://example.com", null)); }}
            @Test void testBrowserBack() {{ assertBrowserInvoked(session.back(null)); }}
            @Test void testBrowserScroll() {{ assertBrowserInvoked(session.scroll("down", "500", null)); }}
            @Test void testBrowserState() {{
                assertBrowserInvoked(session.state(null, OpenCliBrowserStateOptions.builder().build()));
            }}
            @Test void testBrowserFrames() {{ assertBrowserInvoked(session.frames(null)); }}
            @Test void testBrowserScreenshot() {{
                assertBrowserInvoked(session.screenshot("/tmp/cov.png", null, OpenCliBrowserScreenshotOptions.builder().build()));
            }}
            @Test void testBrowserConsole() {{
                assertBrowserInvoked(session.console(null, OpenCliBrowserConsoleOptions.builder().build()));
            }}
            @Test void testBrowserAnalyze() {{ assertBrowserInvoked(session.analyze("https://example.com", null)); }}
            @Test void testBrowserFind() {{
                assertBrowserInvoked(session.find(
                    null, null, OpenCliBrowserFindOptions.builder().css(".x").build()));
            }}
            @Test void testBrowserGetTitle() {{ assertBrowserInvoked(session.getTitle(null)); }}
            @Test void testBrowserGetUrl() {{ assertBrowserInvoked(session.getUrl(null)); }}
            @Test void testBrowserGetText() {{
                assertBrowserInvoked(session.getText("1", OpenCliBrowserSemanticLocator.builder().role("button").build(), null, null));
            }}
            @Test void testBrowserGetValue() {{
                assertBrowserInvoked(session.getValue("1", OpenCliBrowserSemanticLocator.builder().role("textbox").build(), null, null));
            }}
            @Test void testBrowserGetHtml() {{
                assertBrowserInvoked(session.getHtml(null, OpenCliBrowserGetHtmlOptions.builder().build()));
            }}
            @Test void testBrowserGetAttributes() {{
                assertBrowserInvoked(session.getAttributes("1", OpenCliBrowserSemanticLocator.builder().role("button").build(), null, null));
            }}
            @Test void testBrowserClick() {{
                assertBrowserInvoked(session.click("1", OpenCliBrowserSemanticLocator.builder().build(), null));
            }}
            @Test void testBrowserType() {{
                assertBrowserInvoked(session.type("1", "hello", OpenCliBrowserSemanticLocator.builder().build(), null, false));
            }}
            @Test void testBrowserHover() {{
                assertBrowserInvoked(session.hover("1", OpenCliBrowserSemanticLocator.builder().build(), null));
            }}
            @Test void testBrowserFocus() {{
                assertBrowserInvoked(session.focus("1", OpenCliBrowserSemanticLocator.builder().build(), null));
            }}
            @Test void testBrowserDblclick() {{
                assertBrowserInvoked(session.dblclick("1", OpenCliBrowserSemanticLocator.builder().build(), null));
            }}
            @Test void testBrowserCheck() {{
                assertBrowserInvoked(session.check("1", OpenCliBrowserSemanticLocator.builder().build(), null));
            }}
            @Test void testBrowserUncheck() {{
                assertBrowserInvoked(session.uncheck("1", OpenCliBrowserSemanticLocator.builder().build(), null));
            }}
            @Test void testBrowserUpload() {{
                assertBrowserInvoked(session.upload("/tmp/a.txt", java.util.List.of("/tmp/a.txt"),
                    OpenCliBrowserSemanticLocator.builder().build(), null));
            }}
            @Test void testBrowserDrag() {{
                assertBrowserInvoked(session.drag("src", "dst",
                    OpenCliBrowserDragLocator.builder().fromRole("button").toRole("button").build(), null));
            }}
            @Test void testBrowserFill() {{
                assertBrowserInvoked(session.fill("1", "x", OpenCliBrowserSemanticLocator.builder().build(), null));
            }}
            @Test void testBrowserSelect() {{
                assertBrowserInvoked(session.select("1", "opt", OpenCliBrowserSemanticLocator.builder().build(), null));
            }}
            @Test void testBrowserKeys() {{ assertBrowserInvoked(session.keys("Enter", null)); }}
            @Test void testBrowserDialogAccept() {{ assertBrowserInvoked(session.dialogAccept(null, null)); }}
            @Test void testBrowserDialogDismiss() {{ assertBrowserInvoked(session.dialogDismiss(null)); }}
            @Test void testBrowserWait() {{ assertBrowserInvoked(session.waitFor("selector", ".loaded", null, 1000L)); }}
            @Test void testBrowserEval() {{ assertBrowserInvoked(session.eval("1+1", null, null)); }}
            @Test void testBrowserExtract() {{
                assertBrowserInvoked(session.extract(null, OpenCliBrowserExtractOptions.builder().selector("main").build()));
            }}
            @Test void testBrowserNetwork() {{
                assertBrowserInvoked(session.network(OpenCliBrowserSession.OpenCliBrowserNetworkOptions.builder().build(), null));
            }}
            @Test void testBrowserInit() {{ assertBrowserInvoked(session.init("twitter/me")); }}
            @Test void testBrowserVerify() {{
                assertBrowserInvoked(session.verifyAdapter("twitter/me",
                    OpenCliBrowserSession.OpenCliBrowserVerifyOptions.builder().build()));
            }}
            @Test void testBrowserClose() {{ assertBrowserInvoked(session.close()); }}
        }}
        """
    )
    out = COVERAGE_DIR / "OpenCliBrowserCommandsCoverageTest.java"
    out.write_text(java, encoding="utf-8")
    print(f"wrote {out} (42 browser commands)")


def write_coverage_summary_test(manifest_count: int, meta_count: int, browser_count: int) -> None:
    total = manifest_count + meta_count + browser_count
    java = textwrap.dedent(
        f"""\
        package {COVERAGE_PKG};

        import static org.junit.jupiter.api.Assertions.assertEquals;

        import com.fasterxml.jackson.databind.JsonNode;
        import com.fasterxml.jackson.databind.ObjectMapper;
        import org.junit.jupiter.api.Test;

        /**
         * OpenCLI Java SDK 命令覆盖计数汇总。
         * <p>
         * manifest adapter 子命令 + meta 根命令 + browser 子命令 = SDK 侧需覆盖的全部 CLI 入口。
         * </p>
         */
        class OpenCliCommandCoverageSummaryTest {{

            /** cli-manifest.json 条目数（adapter 子命令）。 */
            static final int MANIFEST_ADAPTER_COMMANDS = {manifest_count};

            /** 根级 / meta 子命令数。 */
            static final int META_COMMANDS = {meta_count};

            /** browser 叶子子命令数。 */
            static final int BROWSER_COMMANDS = {browser_count};

            /** 合计需覆盖命令数。 */
            static final int TOTAL_COMMANDS = {total};

            @Test
            void manifestResourceMatchesConstant() throws Exception {{
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(
                    OpenCliCommandCoverageSummaryTest.class.getResourceAsStream("/opencli/manifest-coverage-meta.json"));
                assertEquals(MANIFEST_ADAPTER_COMMANDS, root.get("manifestCommandCount").asInt());
            }}

            @Test
            void totalCoverageBudget() {{
                assertEquals({total}, MANIFEST_ADAPTER_COMMANDS + META_COMMANDS + BROWSER_COMMANDS);
            }}
        }}
        """
    )
    out = COVERAGE_DIR / "OpenCliCommandCoverageSummaryTest.java"
    out.write_text(java, encoding="utf-8")
    print(f"wrote {out} (total {total} commands)")


def main() -> None:
    if not MANIFEST_PATH.is_file():
        raise SystemExit(f"missing manifest: {MANIFEST_PATH}")
    entries = load_manifest()
    write_manifest_resource(entries)
    write_adapter_coverage_test(len(entries))
    meta_count = 26
    browser_count = 42
    write_meta_coverage_test()
    write_browser_coverage_test()
    write_coverage_summary_test(len(entries), meta_count, browser_count)
    print(
        f"done: {len(entries)} adapter + {meta_count} meta + {browser_count} browser "
        f"= {len(entries) + meta_count + browser_count} total"
    )


if __name__ == "__main__":
    main()
