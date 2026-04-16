package com.android.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Excel工具类
 * 用于Excel的导入导出
 *
 * @author sjt
 * @since 2026-01-16
 */
public class ExcelUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建Excel工作簿
     */
    public static Workbook createWorkbook() {
        return new XSSFWorkbook();
    }

    /**
     * 读取Excel文件
     */
    public static Workbook readWorkbook(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        return new XSSFWorkbook(inputStream);
    }

    /**
     * 创建Sheet
     */
    public static Sheet createSheet(Workbook workbook, String sheetName) {
        return workbook.createSheet(sheetName);
    }

    /**
     * 创建表头行
     */
    public static Row createHeaderRow(Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }

        return headerRow;
    }

    /**
     * 创建数据行
     */
    public static Row createDataRow(Sheet sheet, int rowIndex, Object[] values) {
        Row row = sheet.createRow(rowIndex);
        CellStyle dataStyle = sheet.getWorkbook().createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            if (values[i] != null) {
                if (values[i] instanceof Number) {
                    cell.setCellValue(((Number) values[i]).doubleValue());
                } else if (values[i] instanceof Date) {
                    cell.setCellValue(DATE_TIME_FORMATTER.format((Date) values[i]));
                } else if (values[i] instanceof LocalDate) {
                    cell.setCellValue(((LocalDate) values[i]).format(DATE_FORMATTER));
                } else {
                    cell.setCellValue(values[i].toString());
                }
            }
            cell.setCellStyle(dataStyle);
        }

        return row;
    }

    /**
     * 读取单元格值
     */
    public static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DATE_TIME_FORMATTER.format(cell.getDateCellValue());
                } else {
                    // 处理数字，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 读取行数据
     */
    public static List<String> readRowData(Row row, int columnCount) {
        List<String> rowData = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            Cell cell = row.getCell(i);
            rowData.add(getCellValue(cell));
        }
        return rowData;
    }

    /**
     * 导出Excel到响应流
     */
    public static void exportToResponse(HttpServletResponse response, Workbook workbook, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);

        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 解析日期字符串
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析布尔值
     */
    public static Boolean parseBoolean(String boolStr) {
        if (boolStr == null || boolStr.trim().isEmpty()) {
            return null;
        }
        String lower = boolStr.trim().toLowerCase();
        return "是".equals(boolStr) || "true".equals(lower) || "1".equals(boolStr) || "yes".equals(lower);
    }

    /**
     * 解析JSON数组字符串
     */
    public static List<String> parseJsonArray(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty() || "无".equals(jsonStr.trim())) {
            return new ArrayList<>();
        }
        try {
            // 简单的JSON数组解析，支持 ["item1","item2"] 或 item1,item2 格式
            jsonStr = jsonStr.trim();
            if (jsonStr.startsWith("[") && jsonStr.endsWith("]")) {
                jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
            }
            if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
            }
            String[] items = jsonStr.split(",");
            List<String> result = new ArrayList<>();
            for (String item : items) {
                item = item.trim();
                if (item.startsWith("\"") && item.endsWith("\"")) {
                    item = item.substring(1, item.length() - 1);
                }
                if (!item.isEmpty()) {
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
