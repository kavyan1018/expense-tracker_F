package com.expensetracker.web;

import com.expensetracker.domain.TxnType;
import com.expensetracker.dto.ReportDtos.CategoryTotal;
import com.expensetracker.dto.ReportDtos.MonthTotal;
import com.expensetracker.dto.ReportDtos.Summary;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.security.CurrentUser;
import com.expensetracker.service.ExportService;
import com.expensetracker.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reports;
    private final ExportService exports;

    public ReportController(ReportService reports, ExportService exports) {
        this.reports = reports;
        this.exports = exports;
    }

    @GetMapping("/summary")
    public Summary summary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.summary(CurrentUser.id(), from, to);
    }

    @GetMapping("/by-category")
    public List<CategoryTotal> byCategory(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(defaultValue = "EXPENSE") TxnType type) {
        return reports.byCategory(CurrentUser.id(), from, to, type);
    }

    @GetMapping("/monthly-trend")
    public List<MonthTotal> monthlyTrend(@RequestParam(defaultValue = "12") int months) {
        return reports.monthlyTrend(CurrentUser.id(), months);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "xlsx") String format) {
        Long userId = CurrentUser.id();
        String base = "expenses_" + from + "_to_" + to;
        return switch (format.toLowerCase()) {
            case "xlsx" -> download(exports.excel(userId, from, to), XLSX, base + ".xlsx");
            case "pdf" -> download(exports.pdf(userId, from, to), MediaType.APPLICATION_PDF, base + ".pdf");
            default -> throw new BadRequestException("Format must be xlsx or pdf");
        };
    }

    private static ResponseEntity<byte[]> download(byte[] body, MediaType type, String filename) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
