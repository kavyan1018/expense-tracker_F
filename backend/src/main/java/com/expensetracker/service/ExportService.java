package com.expensetracker.service;

import com.expensetracker.domain.Transaction;
import com.expensetracker.domain.TxnType;
import com.expensetracker.dto.ReportDtos.CategoryTotal;
import com.expensetracker.dto.ReportDtos.Summary;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String EXCEL_MONEY = "\"₹\"#,##0.00";

    private final TransactionRepository transactions;
    private final ReportService reports;
    private final UserRepository users;

    public ExportService(TransactionRepository transactions, ReportService reports, UserRepository users) {
        this.transactions = transactions;
        this.reports = reports;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public byte[] excel(Long userId, LocalDate from, LocalDate to) {
        ReportData d = load(userId, from, to);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            bold.setFont(boldFont);
            CellStyle title = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title.setFont(titleFont);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat(EXCEL_MONEY));
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(wb.createDataFormat().getFormat("dd-mmm-yyyy"));

            Sheet summary = wb.createSheet("Summary");
            int r = 0;
            cell(summary.createRow(r++), 0, "Daily Expenses Track: expense report for " + d.name, title);
            cell(summary.createRow(r++), 0, "Period: " + DATE.format(from) + " to " + DATE.format(to), null);
            r++;
            r = moneyRow(summary, r, "Total income", d.summary.income(), bold, money);
            r = moneyRow(summary, r, "Total expenses", d.summary.expense(), bold, money);
            r = moneyRow(summary, r, "Balance", d.summary.balance(), bold, money);
            r++;
            Row h = summary.createRow(r++);
            cell(h, 0, "Expenses by category", bold);
            cell(h, 1, "Amount", bold);
            cell(h, 2, "Share", bold);
            for (CategoryTotal c : d.byCategory) {
                Row row = summary.createRow(r++);
                cell(row, 0, c.name(), null);
                Cell amount = row.createCell(1);
                amount.setCellValue(c.total().doubleValue());
                amount.setCellStyle(money);
                cell(row, 2, c.percent() + "%", null);
            }
            summary.setColumnWidth(0, 30 * 256);
            summary.setColumnWidth(1, 18 * 256);
            summary.setColumnWidth(2, 10 * 256);

            Sheet sheet = wb.createSheet("Transactions");
            Row header = sheet.createRow(0);
            String[] cols = {"Date", "Type", "Category", "Paid by", "Note", "Amount"};
            for (int i = 0; i < cols.length; i++) {
                cell(header, i, cols[i], bold);
            }
            int i = 1;
            for (Transaction t : d.rows) {
                Row row = sheet.createRow(i++);
                Cell dc = row.createCell(0);
                dc.setCellValue(t.getTxnDate());
                dc.setCellStyle(date);
                cell(row, 1, t.getType() == TxnType.INCOME ? "Income" : "Expense", null);
                cell(row, 2, t.getCategory().getName(), null);
                cell(row, 3, t.getPaymentMethod() == null ? "" : t.getPaymentMethod().label(), null);
                cell(row, 4, t.getNote() == null ? "" : t.getNote(), null);
                Cell amount = row.createCell(5);
                amount.setCellValue(t.getAmount().doubleValue());
                amount.setCellStyle(money);
            }
            sheet.createFreezePane(0, 1);
            if (i > 1) {
                sheet.setAutoFilter(new CellRangeAddress(0, i - 1, 0, cols.length - 1));
            }
            int[] widths = {14, 10, 22, 14, 40, 16};
            for (int c = 0; c < widths.length; c++) {
                sheet.setColumnWidth(c, widths[c] * 256);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Long userId, LocalDate from, LocalDate to) {
        ReportData d = load(userId, from, to);
        return PdfReport.render(d.name, d.summary, d.byCategory, d.rows);
    }

    private ReportData load(Long userId, LocalDate from, LocalDate to) {
        ReportService.checkRange(from, to);
        String name = users.findById(userId).map(u -> u.getDisplayName()).orElse("");
        return new ReportData(name, reports.summary(userId, from, to),
                reports.byCategory(userId, from, to, TxnType.EXPENSE),
                transactions.findForExport(userId, from, to));
    }

    private static int moneyRow(Sheet s, int r, String label, BigDecimal value, CellStyle bold, CellStyle money) {
        Row row = s.createRow(r);
        cell(row, 0, label, bold);
        Cell c = row.createCell(1);
        c.setCellValue(value.doubleValue());
        c.setCellStyle(money);
        return r + 1;
    }

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        if (style != null) {
            c.setCellStyle(style);
        }
    }

    private record ReportData(String name, Summary summary, List<CategoryTotal> byCategory, List<Transaction> rows) {
    }
}
