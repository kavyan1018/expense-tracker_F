package com.expensetracker.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryNamesTest {

    @Test
    void pdfDropsEmojiButKeepsText() {
        assertThat(PdfReport.plain("👨‍👩‍👧 Family & Personal")).isEqualTo("Family & Personal");
        assertThat(PdfReport.plain("🛍️ Shopping")).isEqualTo("Shopping");
        assertThat(PdfReport.plain("₹ 500 at Café")).isEqualTo("₹ 500 at Café");
    }

    @Test
    void sortingIgnoresLeadingEmojiAndPutsCatchAllsLast() {
        assertThat(CategoryService.sortKey("🛒 Groceries")).isEqualTo("groceries");
        assertThat(CategoryService.sortKey("📦 Miscellaneous")).isGreaterThan(CategoryService.sortKey("🏠 Rent & Housing"));
        assertThat(CategoryService.sortKey("Other")).isGreaterThan(CategoryService.sortKey("Salary"));
    }
}
