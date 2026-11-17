package com.laminar.ops.system.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class FileCheckExportService {

    private static final String[] HEADERS = {"等级", "表", "记录ID", "字段", "类型", "公司", "医院", "文件路径", "状态", "SLA截止", "首次发现", "恢复时间"};

    @Value("${laminar.file-check.pdf-font:}")
    private String pdfFont;

    public byte[] excel(List<Map<String, Object>> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("文件巡检");
            CellStyle head = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font hf = wb.createFont();
            hf.setBold(true);
            head.setFont(hf);
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(head);
                sheet.setColumnWidth(i, i == 7 ? 9000 : 4000);
            }
            int r = 1;
            for (Map<String, Object> row : rows) {
                Row line = sheet.createRow(r++);
                line.createCell(0).setCellValue(value(row, "severity"));
                line.createCell(1).setCellValue(value(row, "tableName"));
                line.createCell(2).setCellValue(value(row, "recordId"));
                line.createCell(3).setCellValue(value(row, "fileColumn"));
                line.createCell(4).setCellValue(value(row, "fileType"));
                line.createCell(5).setCellValue(value(row, "companyName"));
                line.createCell(6).setCellValue(value(row, "hospitalName"));
                line.createCell(7).setCellValue(value(row, "filePath"));
                line.createCell(8).setCellValue("0".equals(value(row, "status")) ? "丢失" : "已恢复");
                line.createCell(9).setCellValue(value(row, "slaDeadline"));
                line.createCell(10).setCellValue(value(row, "firstSeen"));
                line.createCell(11).setCellValue(value(row, "resolvedTime"));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("导出 Excel 失败：" + e.getMessage(), e);
        }
    }

    public byte[] pdf(List<Map<String, Object>> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, out);
            doc.open();
            BaseFont base = baseFont();
            Font title = new Font(base, 15, Font.BOLD);
            doc.add(new Paragraph("文件巡检报告", title));
            doc.add(new Paragraph("共 " + rows.size() + " 条", new Font(base, 9)));
            PdfPTable table = new PdfPTable(new float[]{1.1f, 2f, 1.1f, 1.3f, 1.8f, 2.8f, 1.2f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            for (String h : new String[]{"等级", "表", "记录", "类型", "公司/医院", "文件路径", "状态", "SLA"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, new Font(base, 8, Font.BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }
            Font small = new Font(base, 7);
            for (Map<String, Object> row : rows) {
                add(table, value(row, "severity"), small);
                add(table, value(row, "tableName"), small);
                add(table, value(row, "recordId"), small);
                add(table, value(row, "fileType"), small);
                add(table, value(row, "companyName") + " / " + value(row, "hospitalName"), small);
                add(table, value(row, "filePath"), small);
                add(table, "0".equals(value(row, "status")) ? "丢失" : "已恢复", small);
                add(table, value(row, "slaDeadline"), small);
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("导出 PDF 失败：" + e.getMessage(), e);
        }
    }

    private BaseFont baseFont() throws Exception {
        for (String candidate : new String[]{pdfFont, "C:/Windows/Fonts/msyh.ttc,0", "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0"}) {
            if (candidate == null || candidate.isBlank()) continue;
            String file = candidate.contains(",") ? candidate.substring(0, candidate.lastIndexOf(',')) : candidate;
            if (Files.isRegularFile(Path.of(file))) {
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }
        return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
    }

    private void add(PdfPTable table, String text, Font font) {
        table.addCell(new PdfPCell(new Phrase(text == null ? "" : text, font)));
    }

    private String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
