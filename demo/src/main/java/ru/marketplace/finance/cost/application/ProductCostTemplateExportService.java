package ru.marketplace.finance.cost.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ProductCostTemplateExportService {

	public byte[] exportTemplate() {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Product costs");
			CellStyle headerStyle = headerStyle(workbook);

			Row header = sheet.createRow(0);
			createHeaderCell(header, 0, "nm_id", headerStyle);
			createHeaderCell(header, 1, "product_name", headerStyle);
			createHeaderCell(header, 2, "cost_price", headerStyle);
			createHeaderCell(header, 3, "effective_from", headerStyle);

			Row example = sheet.createRow(1);
			example.createCell(0).setCellValue(125167917);
			example.createCell(1).setCellValue("Product name example");
			example.createCell(2).setCellValue(650.00);
			example.createCell(3).setCellValue("2026-06-01");

			for (int column = 0; column < 4; column++) {
				sheet.autoSizeColumn(column);
			}
			workbook.write(output);
			return output.toByteArray();
		}
		catch (IOException exception) {
			throw new IllegalStateException("Product cost template cannot be created", exception);
		}
	}

	private static void createHeaderCell(Row row, int column, String value, CellStyle style) {
		row.createCell(column).setCellValue(value);
		row.getCell(column).setCellStyle(style);
	}

	private static CellStyle headerStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		Font font = workbook.createFont();
		font.setBold(true);
		style.setFont(font);
		return style;
	}
}
