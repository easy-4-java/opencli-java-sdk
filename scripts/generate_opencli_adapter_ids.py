#!/usr/bin/env python3
"""从 opencli docs/adapters/index.md 生成 OpenCliAdapterIds.java（请勿手改生成文件）。"""
from __future__ import annotations

import re
from pathlib import Path


def const_name(adapter_id: str) -> str:
    base = adapter_id.replace("-", "_").upper()
    if base and base[0].isdigit():
        return "ADAPTER_" + base
    return base


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    index = Path("/Users/wandl/workspaces/workspace-partme-ai/opencli/opencli/docs/adapters/index.md")
    if not index.is_file():
        raise SystemExit(f"missing index: {index}")
    text = index.read_text(encoding="utf-8")
    browser = set(re.findall(r"\]\(\./browser/([^)]+)\.md\)", text))
    desktop = set(re.findall(r"\]\(\./desktop/([^)]+)\.md\)", text))
    all_ids = sorted(browser | desktop)

    out = root / "src/main/java/io/github/hiwepy/opencli/registry/OpenCliAdapterIds.java"
    out.parent.mkdir(parents=True, exist_ok=True)

    lines = [
        "package io.github.hiwepy.opencli.registry;",
        "",
        "/**",
        " * OpenCLI 适配器标识常量（由脚本从 docs/adapters/index.md 生成）。",
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
