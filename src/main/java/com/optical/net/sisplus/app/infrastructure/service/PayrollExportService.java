package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.domain.PayrollCalculation;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.adapter.PortCaseAdapter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class PayrollExportService {

    private final PortCaseAdapter portCaseAdapter;

    public PayrollExportService(PortCaseAdapter portCaseAdapter) {
        this.portCaseAdapter = portCaseAdapter;
    }

    public Resource generatePayrollExcel(int month, int year) throws IOException {
        List<UserDomain> users = portCaseAdapter.getAllUsers();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Nómina " + getMonthName(month) + " " + year);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle subHeaderStyle = createSubHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle hoursStyle = createHoursStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);

            int rowNum = 0;

            rowNum = createTitle(sheet, rowNum, headerStyle, month, year);
            rowNum = createHeaders(sheet, rowNum, subHeaderStyle);

            for (UserDomain user : users) {
                UserDomain userWithAttendances = portCaseAdapter.findUserById(user.getId());
                PayrollCalculation payroll = userWithAttendances.calculateMonthlyPayroll(month, year);
                rowNum = createEmployeeRow(sheet, rowNum, userWithAttendances, payroll, dataStyle, hoursStyle, currencyStyle);
            }

            rowNum = createTotalsRow(sheet, rowNum, users.size(), totalStyle, currencyStyle);

            autoSizeColumns(sheet);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    private int createTitle(Sheet sheet, int rowNum, CellStyle headerStyle, int month, int year) {
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE DE NÓMINA - " + getMonthName(month).toUpperCase() + " " + year);
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 11));

        Row dateRow = sheet.createRow(rowNum++);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("Fecha de generación: " + LocalDate.now().toString());
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 11));

        sheet.createRow(rowNum++);
        return rowNum;
    }

    private int createHeaders(Sheet sheet, int rowNum, CellStyle subHeaderStyle) {
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {
                "ID", "Nombre", "Apellido", "Cédula",
                "Horas Regulares", "Horas Extras Diurnas", "Horas Extras Nocturnas", "Horas Nocturnas",
                "Pago Regular", "Pago H.E. Diurnas", "Pago H.E. Nocturnas", "Recargo Nocturno",
                "Total Horas Extras", "TOTAL A PAGAR"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(subHeaderStyle);
        }

        return rowNum;
    }

    private int createEmployeeRow(Sheet sheet, int rowNum, UserDomain user, PayrollCalculation payroll,
                                  CellStyle dataStyle, CellStyle hoursStyle, CellStyle currencyStyle) {
        Row row = sheet.createRow(rowNum++);

        createCell(row, 0, user.getId(), dataStyle);
        createCell(row, 1, user.getName(), dataStyle);
        createCell(row, 2, user.getLastName(), dataStyle);
        createCell(row, 3, user.getCc(), dataStyle);

        createCell(row, 4, payroll.getRegularHours(), hoursStyle);
        createCell(row, 5, payroll.getDayOvertimeHours(), hoursStyle);
        createCell(row, 6, payroll.getNightOvertimeHours(), hoursStyle);
        createCell(row, 7, payroll.getNightHours(), hoursStyle);

        createCell(row, 8, payroll.getRegularPay(), currencyStyle);
        createCell(row, 9, payroll.getDayOvertimePay(), currencyStyle);
        createCell(row, 10, payroll.getNightOvertimePay(), currencyStyle);
        createCell(row, 11, payroll.getNightSurchargePay(), currencyStyle);

        createCell(row, 12, payroll.getTotalOvertimePay(), currencyStyle);
        createCell(row, 13, payroll.getTotalPay(), currencyStyle);

        return rowNum;
    }

    private int createTotalsRow(Sheet sheet, int rowNum, int employeeCount, CellStyle totalStyle, CellStyle currencyStyle) {
        Row totalRow = sheet.createRow(rowNum++);

        Cell labelCell = totalRow.createCell(0);
        labelCell.setCellValue("TOTALES");
        labelCell.setCellStyle(totalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

        int dataStartRow = 5;
        int dataEndRow = 4 + employeeCount;

        createFormulaCell(totalRow, 4, "SUM(E" + dataStartRow + ":E" + dataEndRow + ")", totalStyle);
        createFormulaCell(totalRow, 5, "SUM(F" + dataStartRow + ":F" + dataEndRow + ")", totalStyle);
        createFormulaCell(totalRow, 6, "SUM(G" + dataStartRow + ":G" + dataEndRow + ")", totalStyle);
        createFormulaCell(totalRow, 7, "SUM(H" + dataStartRow + ":H" + dataEndRow + ")", totalStyle);

        CellStyle totalCurrencyStyle = sheet.getWorkbook().createCellStyle();
        totalCurrencyStyle.cloneStyleFrom(totalStyle);
        totalCurrencyStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("$#,##0.00"));

        createFormulaCell(totalRow, 8, "SUM(I" + dataStartRow + ":I" + dataEndRow + ")", totalCurrencyStyle);
        createFormulaCell(totalRow, 9, "SUM(J" + dataStartRow + ":J" + dataEndRow + ")", totalCurrencyStyle);
        createFormulaCell(totalRow, 10, "SUM(K" + dataStartRow + ":K" + dataEndRow + ")", totalCurrencyStyle);
        createFormulaCell(totalRow, 11, "SUM(L" + dataStartRow + ":L" + dataEndRow + ")", totalCurrencyStyle);
        createFormulaCell(totalRow, 12, "SUM(M" + dataStartRow + ":M" + dataEndRow + ")", totalCurrencyStyle);
        createFormulaCell(totalRow, 13, "SUM(N" + dataStartRow + ":N" + dataEndRow + ")", totalCurrencyStyle);

        return rowNum;
    }

    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        }
        cell.setCellStyle(style);
    }

    private void createFormulaCell(Row row, int column, String formula, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellFormula(formula);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle createHoursStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        return style;
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 14; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 1000);
        }
    }

    private String getMonthName(int month) {
        String[] months = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        return months[month - 1];
    }
}