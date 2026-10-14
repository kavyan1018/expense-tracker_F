package com.expensetracker.service;

import com.expensetracker.domain.Transaction;
import com.expensetracker.domain.TxnType;
import com.expensetracker.dto.ReportDtos.CategoryTotal;
import com.expensetracker.dto.ReportDtos.Summary;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Renders the PDF report. The built-in PDF fonts have no rupee sign, so amounts are shown as "Rs." */
final class PdfReport {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Color HEADER_BG = new Color(241, 245, 249);
    private static final Color INCOME = new Color(5, 150, 105);
    private static final Color EXPENSE = new Color(220, 38, 38);

    private PdfReport() {
    }

    static byte[] render(String name, Summary summary, List<CategoryTotal> byCategory, List<Transaction> rows) {
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font muted = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, out);
        doc.open();

        doc.add(new Paragraph("Expense report", title));
        doc.add(new Paragraph(name + "  |  " + DATE.format(summary.from()) + " to " + DATE.format(summary.to()), muted));
        doc.add(spacer());

        PdfPTable totals = new PdfPTable(3);
        totals.setWidthPercentage(100);
        totals.addCell(totalCell("Income", summary.income(), INCOME));
        totals.addCell(totalCell("Expenses", summary.expense(), EXPENSE));
        totals.addCell(totalCell("Balance", summary.balance(),
                summary.balance().signum() < 0 ? EXPENSE : Color.BLACK));
        doc.add(totals);
        doc.add(spacer());

        doc.add(new Paragraph("Expenses by category", h2));
        doc.add(spacer());
        PdfPTable cats = new PdfPTable(new float[]{5, 3, 2});
        cats.setWidthPercentage(70);
        cats.setHorizontalAlignment(Element.ALIGN_LEFT);
        header(cats, bold, "Category", "Amount", "Share");
        if (byCategory.isEmpty()) {
            PdfPCell none = new PdfPCell(new Phrase("No expenses in this period", body));
            none.setColspan(3);
            cats.addCell(none);
        }
        for (CategoryTotal c : byCategory) {
            cats.addCell(text(c.name(), body));
            cats.addCell(right(money(c.total()), body));
            cats.addCell(right(c.percent() + "%", body));
        }
        doc.add(cats);
        doc.add(spacer());

        doc.add(new Paragraph("Transactions (" + rows.size() + ")", h2));
        doc.add(spacer());
        PdfPTable txns = new PdfPTable(new float[]{2.2f, 1.6f, 2.6f, 5f, 2.4f});
        txns.setWidthPercentage(100);
        txns.setHeaderRows(1);
        header(txns, bold, "Date", "Type", "Category", "Note", "Amount");
        for (Transaction t : rows) {
            boolean income = t.getType() == TxnType.INCOME;
            txns.addCell(text(DATE.format(t.getTxnDate()), body));
            txns.addCell(text(income ? "Income" : "Expense", body));
            txns.addCell(text(t.getCategory().getName(), body));
            txns.addCell(text(t.getNote() == null ? "" : t.getNote(), body));
            Font amountFont = FontFactory.getFont(FontFactory.HELVETICA, 9, income ? INCOME : Color.BLACK);
            txns.addCell(right((income ? "+" : "-") + money(t.getAmount()), amountFont));
        }
        doc.add(txns);

        Paragraph footer = new Paragraph("Generated " + DATE.format(LocalDate.now()), muted);
        footer.setSpacingBefore(12);
        doc.add(footer);
        doc.close();
        return out.toByteArray();
    }

    static String money(BigDecimal amount) {
        return "Rs. " + new DecimalFormat("#,##0.00").format(amount);
    }

    private static Paragraph spacer() {
        Paragraph p = new Paragraph(" ");
        p.setLeading(6);
        return p;
    }

    private static void header(PdfPTable table, Font font, String... labels) {
        for (String label : labels) {
            PdfPCell c = new PdfPCell(new Phrase(label, font));
            c.setBackgroundColor(HEADER_BG);
            c.setPadding(5);
            table.addCell(c);
        }
    }

    private static PdfPCell text(String value, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(value, font));
        c.setPadding(4);
        return c;
    }

    private static PdfPCell right(String value, Font font) {
        PdfPCell c = text(value, font);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private static PdfPCell totalCell(String label, BigDecimal value, Color color) {
        PdfPCell c = new PdfPCell();
        c.setPadding(8);
        c.addElement(new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY)));
        c.addElement(new Paragraph(money(value), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, color)));
        return c;
    }
}
