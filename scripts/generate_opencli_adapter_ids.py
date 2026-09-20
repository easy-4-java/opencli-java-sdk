#!/usr/bin/env python3
"""从 opencli docs/adapters/index.md 与 cli-manifest.json 生成 OpenCliAdapterIds.java（请勿手改生成文件）。"""
from __future__ import annotations

import json
import os
import re
from pathlib import Path

# 优先 OPENCLI_ROOT，其次 workspace-partme-ai 与 boot-starters 同级的默认路径。
_SCRIPT_DIR = Path(__file__).resolve().parent
_DEFAULT_RELATIVE = _SCRIPT_DIR.parents[4] / "workspace-partme-ai" / "opencli" / "opencli"
if not _DEFAULT_RELATIVE.is_dir():
    _DEFAULT_RELATIVE = _SCRIPT_DIR.parents[3] / ".." / "workspace-partme-ai" / "opencli" / "opencli"
OPENCLI_ROOT = Path(os.environ.get("OPENCLI_ROOT", _DEFAULT_RELATIVE)).resolve()

# 文档链接 slug 与 CLI registry site 不一致时的映射（以 manifest 为准）。
DOC_TO_MANIFEST_RENAMES: dict[str, str] = {
    "discord": "discord-app",
}


def normalize_id(adapter_id: str) -> str:
    return DOC_TO_MANIFEST_RENAMES.get(adapter_id, adapter_id)


def const_name(adapter_id: str) -> str:
    base = adapter_id.replace("-", "_").upper()
    if base and base[0].isdigit():
        return "ADAPTER_" + base
    return base


def load_manifest_sites() -> set[str]:
    manifest = OPENCLI_ROOT / "cli-manifest.json"
    if not manifest.is_file():
        return set()
    data = json.loads(manifest.read_text(encoding="utf-8"))
    return {entry["site"] for entry in data}


# 缺 docs/adapters/index.md 时，从 clis/ 目录启发式推断 desktop app。
# 已知的 desktop 候选 id（与 clis/ 子目录一一对应）：
_KNOWN_DESKTOP_IDS = {
    "antigravity", "codex", "cursor", "qoder",
    "trae-cn", "trae-solo",
    "chatgpt-app", "chatwise", "discord-app", "doubao-app",
}


def infer_desktop_ids_from_clis_dir() -> set[str]:
    clis_dir = OPENCLI_ROOT / "clis"
    if not clis_dir.is_dir():
        return set()
    found = set()
    for entry in clis_dir.iterdir():
        if not entry.is_dir() or entry.name.startswith(("_", ".")):
            continue
        if entry.name in _KNOWN_DESKTOP_IDS or entry.name.endswith("-app"):
            found.add(entry.name)
    return found


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    index = OPENCLI_ROOT / "docs/adapters/index.md"
    browser: set[str] = set()
    desktop: set[str] = set()
    if index.is_file():
        text = index.read_text(encoding="utf-8")
        browser = {normalize_id(x) for x in re.findall(r"\]\(\./browser/([^)]+)\.md\)", text)}
        desktop = {normalize_id(x) for x in re.findall(r"\]\(\./desktop/([^)]+)\.md\)", text)}
    else:
        print(f"warning: index not found at {index}; falling back to manifest + clis/ heuristic")
        desktop = infer_desktop_ids_from_clis_dir()
    manifest_sites = load_manifest_sites()
    # manifest 为 CLI 真值；文档 index 补充分类与尚未入 manifest 的条目。
    all_ids = sorted(manifest_sites | browser | desktop)
    # 重新划分 browser：未命中 desktop 的 manifest / index 条目一律视为 browser 类。
    browser = {x for x in all_ids if x not in desktop}

    out = root / "src/main/java/io/github/hiwepy/opencli/registry/OpenCliAdapterIds.java"
    out.parent.mkdir(parents=True, exist_ok=True)

    lines = [
        "package io.github.hiwepy.opencli.registry;",
        "",
        "/**",
        " * OpenCLI 适配器标识常量（由脚本从 docs/adapters/index.md + cli-manifest.json 生成）。",
        " * <p>运行 {@code scripts/generate_opencli_adapter_ids.py} 刷新。</p>",
        " */",
        "public final class OpenCliAdapterIds {",
        "",
        "    private OpenCliAdapterIds() {",
        "    }",
        "",
    ]
    const_pairs = [(aid, const_name(aid)) for aid in all_ids]
    for aid, cname in const_pairs:
        lines.append(f'    public static final String {cname} = "{aid}";')
    lines.append("")
    lines.append("    /** 文档索引中的全部 adapter id（便于遍历集成）。 */")
    lines.append("    public static final String[] ALL = {")
    for aid, cname in const_pairs:
        lines.append(f"        {cname},")
    lines.append("    };")
    lines.extend(
        [
            "",
            "    /** Browser 类适配器数量（文档 index 链接口径）。 */",
            f"    public static final int BROWSER_ADAPTER_COUNT = {len(browser)};",
            "",
            "    /** Desktop 类适配器数量。 */",
            f"    public static final int DESKTOP_ADAPTER_COUNT = {len(desktop)};",
            "",
            "    /** 文档索引中的适配器总数（browser ∪ desktop）。 */",
            f"    public static final int TOTAL_ADAPTER_COUNT = {len(all_ids)};",
            "}",
            "",
        ]
    )
    out.write_text("\n".join(lines), encoding="utf-8")
    print(f"wrote {out} ({len(all_ids)} adapters)")

    tax = root / "src/main/java/io/github/hiwepy/opencli/registry/OpenCliAdapterTaxonomy.java"
    desk_sorted = sorted(desktop)
    brows_sorted = sorted(browser)
    tax_lines = [
        "package io.github.hiwepy.opencli.registry;",
        "",
        "import java.util.Collections;",
        "import java.util.Arrays;",
        "import java.util.HashSet;",
        "import java.util.List;",
        "import java.util.Set;",
        "",
        "/**",
        " * 文档 index 中的 Browser / Desktop 分类（由脚本生成）。",
        " */",
        "public final class OpenCliAdapterTaxonomy {",
        "",
        "    private OpenCliAdapterTaxonomy() {",
        "    }",
        "",
        "    /** Desktop 适配器 id 列表（固定集）。 */",
        "    public static final List<String> DESKTOP_IDS = Collections.unmodifiableList(Arrays.asList(",
    ]
    for i, d in enumerate(desk_sorted):
        line = f'        "{d}"'
        if i < len(desk_sorted) - 1:
            line += ","
        tax_lines.append(line)
    tax_lines.extend(
        [
            "    ));",
            "",
            "    /** Browser 文档链接中的适配器 id。 */",
            "    public static final List<String> BROWSER_IDS = Collections.unmodifiableList(Arrays.asList(",
        ]
    )
    for i, b in enumerate(brows_sorted):
        line = f'        "{b}"'
        if i < len(brows_sorted) - 1:
            line += ","
        tax_lines.append(line)
    tax_lines.extend(
        [
            "    ));",
            "",
            "    private static final Set<String> DESKTOP_ID_SET = new HashSet<>(DESKTOP_IDS);",
            "",
            "    /**",
            "     * @param adapterId OpenCLI adapter id",
            "     * @return 是否属于 Desktop 表",
            "     */",
            "    public static boolean isDesktopAdapter(String adapterId) {",
            "        return adapterId != null && DESKTOP_ID_SET.contains(adapterId);",
            "    }",
            "}",
            "",
        ]
    )
    tax.write_text("\n".join(tax_lines), encoding="utf-8")
    print(f"wrote {tax}")


if __name__ == "__main__":
    main()
