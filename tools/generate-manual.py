import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "使用说明书.md"
OUTPUT = ROOT / "docs" / "使用说明书.docx"
INK = RGBColor(0x11, 0x11, 0x11)
BODY = RGBColor(0x37, 0x41, 0x51)
MUTED = RGBColor(0x6B, 0x72, 0x80)
ACCENT = RGBColor(0x1F, 0x6F, 0xEB)
HAIRLINE = "E5E7EB"
SOFT = "F8F9FA"


def set_font(run, size=10.5, bold=False, color=BODY, mono=False):
    run.font.name = "Consolas" if mono else "Microsoft YaHei"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color


def shade(cell, color):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), color)
    tc_pr.append(shd)


def cell_margins(table, top=80, bottom=80, left=120, right=120):
    tbl_pr = table._tbl.tblPr
    mar = OxmlElement("w:tblCellMar")
    for name, value in (("top", top), ("left", left), ("bottom", bottom), ("right", right)):
        node = OxmlElement(f"w:{name}")
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")
        mar.append(node)
    tbl_pr.append(mar)


def table_borders(table, color=HAIRLINE):
    tbl_pr = table._tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), "single")
        el.set(qn("w:sz"), "4")
        el.set(qn("w:space"), "0")
        el.set(qn("w:color"), color)
        borders.append(el)
    tbl_pr.append(borders)


def add_inline(paragraph, text, bold=False, size=10.5, color=BODY):
    pattern = re.compile(r"(\*\*.+?\*\*|`.+?`)")
    pos = 0
    for match in pattern.finditer(text):
        if match.start() > pos:
            run = paragraph.add_run(text[pos:match.start()])
            set_font(run, size, bold, color)
        token = match.group(0)
        if token.startswith("**"):
            run = paragraph.add_run(token[2:-2])
            set_font(run, size, True, color)
        else:
            run = paragraph.add_run(token[1:-1])
            set_font(run, size - 0.7, bold, RGBColor(0x24, 0x29, 0x2F), mono=True)
        pos = match.end()
    if pos < len(text):
        run = paragraph.add_run(text[pos:])
        set_font(run, size, bold, color)


def add_page_number(paragraph):
    run = paragraph.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_end)
    set_font(run, 9, False, MUTED)


def add_heading(doc, text, level):
    p = doc.add_paragraph()
    p.style = doc.styles[f"Heading {level}"]
    p.paragraph_format.keep_with_next = True
    p.paragraph_format.space_before = Pt(18 if level == 1 else 12)
    p.paragraph_format.space_after = Pt(8 if level == 1 else 5)
    run = p.add_run(text)
    if level == 1:
        set_font(run, 18, True, INK)
        p.paragraph_format.page_break_before = level == 1 and text not in ("文档说明", "目录")
        p_pr = p._p.get_or_add_pPr()
        border = OxmlElement("w:pBdr")
        bottom = OxmlElement("w:bottom")
        bottom.set(qn("w:val"), "single")
        bottom.set(qn("w:sz"), "8")
        bottom.set(qn("w:space"), "4")
        bottom.set(qn("w:color"), "111111")
        border.append(bottom)
        p_pr.append(border)
    elif level == 2:
        set_font(run, 13.5, True, INK)
    else:
        set_font(run, 11.5, True, INK)
    return p


def add_code_block(doc, code):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = True
    table_borders(table, "EEF2F7")
    cell = table.cell(0, 0)
    shade(cell, "F6F8FA")
    cell_margins(table, 90, 90, 150, 150)
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.15
    run = p.add_run(code.rstrip("\n"))
    set_font(run, 8.8, False, RGBColor(0x24, 0x29, 0x2F), mono=True)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def add_callout(doc, text):
    table = doc.add_table(rows=1, cols=1)
    table.autofit = True
    table_borders(table, "E5E7EB")
    cell = table.cell(0, 0)
    shade(cell, "F8F9FA")
    cell_margins(table, 100, 100, 140, 140)
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    add_inline(p, text, size=10, color=BODY)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def add_image(doc, alt, src):
    path = (SOURCE.parent / src).resolve()
    if not path.exists():
        add_callout(doc, f"图片未找到：{src}")
        return
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(2)
    run = p.add_run()
    run.add_picture(str(path), width=Cm(15.6))
    caption = doc.add_paragraph()
    caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
    caption.paragraph_format.space_after = Pt(10)
    r = caption.add_run(alt)
    set_font(r, 9, False, MUTED)


def add_table(doc, rows):
    if not rows:
        return
    header = [c.strip() for c in rows[0].strip().strip("|").split("|")]
    data = []
    for raw in rows[1:]:
        cells = [c.strip() for c in raw.strip().strip("|").split("|")]
        if cells and all(re.fullmatch(r":?-{2,}:?", c) for c in cells):
            continue
        data.append(cells)
    table = doc.add_table(rows=1 + len(data), cols=len(header))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = True
    table_borders(table)
    cell_margins(table)
    for i, text in enumerate(header):
        cell = table.rows[0].cells[i]
        shade(cell, "111111")
        p = cell.paragraphs[0]
        p.paragraph_format.space_after = Pt(0)
        add_inline(p, text, bold=True, size=9.2, color=RGBColor(0xFF, 0xFF, 0xFF))
    for r_index, cells in enumerate(data, start=1):
        for c_index in range(len(header)):
            cell = table.rows[r_index].cells[c_index]
            if r_index % 2 == 0:
                shade(cell, "F8F9FA")
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.25
            add_inline(p, cells[c_index] if c_index < len(cells) else "", size=9.2)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def add_list(doc, text, ordered=False):
    p = doc.add_paragraph(style="List Number" if ordered else "List Bullet")
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.line_spacing = 1.4
    add_inline(p, text, size=10.5)


def parse_markdown():
    lines = SOURCE.read_text(encoding="utf-8").splitlines()
    # Cover: content before first horizontal rule.
    end = next((i for i, line in enumerate(lines) if line.strip() == "---"), 12)
    cover = lines[:end]
    body = lines[end + 1:]
    return cover, body


def build_cover(doc, cover):
    section = doc.sections[0]
    section.top_margin = Cm(2.0)
    section.bottom_margin = Cm(2.0)
    section.left_margin = Cm(2.0)
    section.right_margin = Cm(2.0)
    section.different_first_page_header_footer = True

    bar = doc.add_table(rows=1, cols=1)
    bar.autofit = True
    bar.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = bar.cell(0, 0)
    shade(cell, "111111")
    cell_margins(bar, 70, 70, 120, 120)
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    r = p.add_run("  LO  /  LAMINAR OPS")
    set_font(r, 10, True, RGBColor(0xFF, 0xFF, 0xFF))

    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(48)
    p.paragraph_format.line_spacing = 1.05
    r = p.add_run("层流净化设备")
    set_font(r, 30, True, INK)

    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.line_spacing = 1.05
    r = p.add_run("智能监测运维一体化平台")
    set_font(r, 30, True, INK)

    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(18)
    p.paragraph_format.space_after = Pt(22)
    p.paragraph_format.line_spacing = 1.05
    r = p.add_run("使用说明书")
    set_font(r, 17, True, ACCENT)

    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(30)
    p.paragraph_format.line_spacing = 1.3
    r = p.add_run("从档案、监测、告警到工单、巡检、文件治理的一体化操作指南")
    set_font(r, 11, False, MUTED)

    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(24)
    p_pr = p._p.get_or_add_pPr()
    border = OxmlElement("w:pBdr")
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"), "single")
    bottom.set(qn("w:sz"), "6")
    bottom.set(qn("w:space"), "1")
    bottom.set(qn("w:color"), "E5E7EB")
    border.append(bottom)
    p_pr.append(border)
    p.add_run("")

    meta = doc.add_table(rows=4, cols=2)
    meta.autofit = True
    meta.alignment = WD_TABLE_ALIGNMENT.LEFT
    table_borders(meta, "E5E7EB")
    cell_margins(meta, 90, 90, 120, 120)
    rows = [
        ("文档版本", "V1.1"),
        ("更新日期", "2026-11-18"),
        ("适用系统", "运维后台 / 院方前台"),
        ("文档类型", "用户操作与业务规则说明"),
    ]
    for i, (key, value) in enumerate(rows):
        c1, c2 = meta.rows[i].cells
        shade(c1, "F8F9FA")
        p1 = c1.paragraphs[0]
        p1.paragraph_format.space_after = Pt(0)
        add_inline(p1, key, bold=True, size=9.5, color=MUTED)
        p2 = c2.paragraphs[0]
        p2.paragraph_format.space_after = Pt(0)
        add_inline(p2, value, size=9.5)

    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(44)
    add_inline(p, "说明：本说明书中的截图使用演示数据，实际页面数据以系统为准。", size=9, color=MUTED)

    doc.add_page_break()


def build_document():
    doc = Document()
    doc.core_properties.title = "层流净化设备智能监测运维一体化平台使用说明书"
    doc.core_properties.author = "Laminar Ops"
    doc.core_properties.subject = "用户操作、权限、业务规则与运维说明"

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Microsoft YaHei"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    normal.font.size = Pt(10.5)
    normal.font.color.rgb = BODY
    normal.paragraph_format.line_spacing = 1.5
    normal.paragraph_format.space_after = Pt(5)

    for level, size in ((1, 18), (2, 13.5), (3, 11.5)):
        style = styles[f"Heading {level}"]
        style.font.name = "Microsoft YaHei"
        style._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = INK

    section = doc.sections[0]
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.0)
    section.bottom_margin = Cm(1.8)
    section.left_margin = Cm(2.1)
    section.right_margin = Cm(2.1)

    # Running header
    header = section.header
    hp = header.paragraphs[0]
    hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    hr = hp.add_run("Laminar Ops  ·  层流净化设备智能监测运维一体化平台")
    set_font(hr, 8.5, False, MUTED)

    # Footer page number
    footer = section.footer
    fp = footer.paragraphs[0]
    fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    add_page_number(fp)

    cover, body = parse_markdown()
    build_cover(doc, cover)

    i = 0
    while i < len(body):
        raw = body[i]
        line = raw.strip()
        if not line:
            i += 1
            continue
        if line == "<!-- pagebreak -->":
            doc.add_page_break()
            i += 1
            continue
        if line == "---":
            i += 1
            continue
        if line.startswith("```"):
            i += 1
            code = []
            while i < len(body) and not body[i].strip().startswith("```"):
                code.append(body[i])
                i += 1
            add_code_block(doc, "\n".join(code))
            i += 1
            continue
        if line.startswith("!["):
            match = re.match(r"!\[(.*?)]\((.*?)\)", line)
            if match:
                add_image(doc, match.group(1), match.group(2))
            i += 1
            continue
        if line.startswith("|"):
            rows = []
            while i < len(body) and body[i].strip().startswith("|"):
                rows.append(body[i].strip())
                i += 1
            add_table(doc, rows)
            continue
        if line.startswith(">"):
            add_callout(doc, line.lstrip("> ").strip())
            i += 1
            continue
        if line.startswith("#"):
            level = len(line) - len(line.lstrip("#"))
            text = line[level:].strip()
            if level == 1:
                add_heading(doc, text, 1)
            elif level == 2:
                add_heading(doc, text, 2)
            else:
                add_heading(doc, text, 3)
            i += 1
            continue
        if re.match(r"^\d+\.\s+", line):
            add_list(doc, re.sub(r"^\d+\.\s+", "", line), ordered=True)
            i += 1
            continue
        if line.startswith("- "):
            add_list(doc, line[2:], ordered=False)
            i += 1
            continue
        p = doc.add_paragraph()
        add_inline(p, line)
        i += 1

    doc.save(OUTPUT)
    print(f"generated: {OUTPUT}")


if __name__ == "__main__":
    try:
        build_document()
    except Exception as exc:
        print(f"failed: {exc}", file=sys.stderr)
        raise
