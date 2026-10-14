package com.expensetracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    String asha;
    String ravi;

    @BeforeEach
    void users() throws Exception {
        asha = register("asha");
        ravi = register("ravi");
    }

    // ---- auth ----

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/transactions").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWorksAndRejectsWrongPassword() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        send(post("/api/auth/register"), null, body("email", email, "password", "password123", "displayName", "L"))
                .andExpect(status().isCreated());
        send(post("/api/auth/login"), null, body("email", email.toUpperCase(), "password", "password123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        send(post("/api/auth/login"), null, body("email", email, "password", "wrong-password"))
                .andExpect(status().isUnauthorized());
        send(post("/api/auth/register"), null, body("email", email, "password", "password123", "displayName", "L"))
                .andExpect(status().isConflict());
    }

    @Test
    void newUsersGetDefaultCategories() throws Exception {
        JsonNode cats = read(send(get("/api/categories"), asha, null));
        assertThat(cats.size()).isEqualTo(13);
    }

    // ---- privacy ----

    @Test
    void usersCannotSeeOrChangeEachOthersRecords() throws Exception {
        long groceries = categoryId(asha, "Groceries");
        long txn = createTxn(asha, groceries, "1200.00", "2026-10-05");
        long budget = read(send(put("/api/budgets"), asha,
                "{\"month\":\"2026-10\",\"categoryId\":" + groceries + ",\"amount\":5000}")).get("id").asLong();
        long rule = createRule(asha, groceries, "2026-11-01");

        // Ravi can't list, read, update or delete any of Asha's records
        send(get("/api/transactions"), ravi, null).andExpect(jsonPath("$.totalElements").value(0));
        send(put("/api/transactions/" + txn), ravi, txnBody(groceries, "1.00", "2026-10-05"))
                .andExpect(status().isNotFound());
        send(delete("/api/transactions/" + txn), ravi, null).andExpect(status().isNotFound());
        send(put("/api/categories/" + groceries), ravi, "{\"name\":\"Mine\",\"type\":\"EXPENSE\"}")
                .andExpect(status().isNotFound());
        send(delete("/api/categories/" + groceries), ravi, null).andExpect(status().isNotFound());
        send(delete("/api/budgets/" + budget), ravi, null).andExpect(status().isNotFound());
        send(get("/api/budgets?month=2026-10"), ravi, null).andExpect(jsonPath("$.length()").value(0));
        send(post("/api/recurring/" + rule + "/pause"), ravi, null).andExpect(status().isNotFound());
        send(delete("/api/recurring/" + rule), ravi, null).andExpect(status().isNotFound());
        send(get("/api/recurring"), ravi, null).andExpect(jsonPath("$.length()").value(0));

        // ...or attach his records to her category
        send(post("/api/transactions"), ravi, txnBody(groceries, "10.00", "2026-10-05"))
                .andExpect(status().isNotFound());
        send(put("/api/budgets"), ravi, "{\"month\":\"2026-10\",\"categoryId\":" + groceries + ",\"amount\":1}")
                .andExpect(status().isNotFound());

        // ...and her money never shows up in his reports
        send(get("/api/reports/summary?from=2026-01-01&to=2026-12-31"), ravi, null)
                .andExpect(jsonPath("$.expense").value(0));

        // Asha's record is untouched
        send(get("/api/transactions"), asha, null)
                .andExpect(jsonPath("$.content[0].amount").value(1200.00));
    }

    @Test
    void databaseRejectsCrossUserCategoryReferences() throws Exception {
        long ashaGroceries = categoryId(asha, "Groceries");
        Long raviId = jdbc.queryForObject("select id from users where email = ?", Long.class, emailOf("ravi"));
        assertThatThrownBy(() -> jdbc.update(
                "insert into transactions (user_id, category_id, type, amount, txn_date) values (?, ?, 'EXPENSE', 5, ?)",
                raviId, ashaGroceries, TODAY)).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---- transactions ----

    @Test
    void transactionValidationAndTypeFollowsCategory() throws Exception {
        long salary = categoryId(asha, "Salary");
        send(post("/api/transactions"), asha, "{\"categoryId\":" + salary + ",\"amount\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists())
                .andExpect(jsonPath("$.fieldErrors.date").exists());
        send(post("/api/transactions"), asha, txnBody(salary, "50000.00", "2026-10-01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void filtersAndSearch() throws Exception {
        long groceries = categoryId(asha, "Groceries");
        long rent = categoryId(asha, "Rent");
        createTxn(asha, groceries, "100.00", "2026-09-10");
        createTxn(asha, rent, "15000.00", "2026-10-01");
        send(post("/api/transactions"), asha,
                "{\"categoryId\":" + groceries + ",\"amount\":250,\"date\":\"2026-10-03\",\"note\":\"Weekly veggies\"}");
        send(get("/api/transactions?from=2026-10-01&to=2026-10-31"), asha, null)
                .andExpect(jsonPath("$.totalElements").value(2));
        send(get("/api/transactions?categoryId=" + groceries), asha, null)
                .andExpect(jsonPath("$.totalElements").value(2));
        send(get("/api/transactions?q=VEGGIES"), asha, null)
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---- categories ----

    @Test
    void deletingAUsedCategoryArchivesIt() throws Exception {
        long groceries = categoryId(asha, "Groceries");
        long health = categoryId(asha, "Health");
        createTxn(asha, groceries, "10.00", "2026-10-01");
        send(delete("/api/categories/" + groceries), asha, null).andExpect(jsonPath("$.archived").value(true));
        send(delete("/api/categories/" + health), asha, null).andExpect(jsonPath("$.deleted").value(true));
        send(post("/api/transactions"), asha, txnBody(groceries, "10.00", "2026-10-02"))
                .andExpect(status().isBadRequest());
    }

    // ---- budgets and reports ----

    @Test
    void budgetsTrackSpendingAndReportsAddUp() throws Exception {
        long groceries = categoryId(asha, "Groceries");
        long rent = categoryId(asha, "Rent");
        long salary = categoryId(asha, "Salary");
        createTxn(asha, salary, "80000.00", "2026-10-01");
        createTxn(asha, rent, "20000.00", "2026-10-02");
        createTxn(asha, groceries, "3000.00", "2026-10-05");
        createTxn(asha, groceries, "2500.50", "2026-10-12");
        createTxn(asha, groceries, "999.00", "2026-09-30"); // previous month

        send(put("/api/budgets"), asha, "{\"month\":\"2026-10\",\"categoryId\":null,\"amount\":40000}");
        send(put("/api/budgets"), asha, "{\"month\":\"2026-10\",\"categoryId\":" + groceries + ",\"amount\":5000}");
        // upsert: same month and category updates instead of duplicating
        send(put("/api/budgets"), asha, "{\"month\":\"2026-10\",\"categoryId\":" + groceries + ",\"amount\":5000}");

        JsonNode budgets = read(send(get("/api/budgets?month=2026-10"), asha, null));
        assertThat(budgets.size()).isEqualTo(2);
        assertThat(budgets.get(0).get("categoryId").isNull()).isTrue();
        assertThat(budgets.get(0).get("spent").decimalValue()).isEqualByComparingTo("25500.50");
        assertThat(budgets.get(1).get("spent").decimalValue()).isEqualByComparingTo("5500.50");
        assertThat(budgets.get(1).get("remaining").decimalValue()).isEqualByComparingTo("-500.50");
        assertThat(budgets.get(1).get("percentUsed").asInt()).isEqualTo(110);

        send(put("/api/budgets"), asha, "{\"month\":\"2026-10\",\"categoryId\":" + salary + ",\"amount\":1}")
                .andExpect(status().isBadRequest());

        send(post("/api/budgets/copy?from=2026-10&to=2026-11"), asha, null)
                .andExpect(jsonPath("$.length()").value(2));

        send(get("/api/reports/summary?from=2026-10-01&to=2026-10-31"), asha, null)
                .andExpect(jsonPath("$.income").value(80000.00))
                .andExpect(jsonPath("$.expense").value(25500.50))
                .andExpect(jsonPath("$.balance").value(54499.50));

        JsonNode byCat = read(send(get("/api/reports/by-category?from=2026-10-01&to=2026-10-31"), asha, null));
        assertThat(byCat.get(0).get("name").asText()).isEqualTo("Rent");
        assertThat(byCat.get(0).get("percent").asDouble()).isEqualTo(78.4);

        JsonNode trend = read(send(get("/api/reports/monthly-trend?months=3"), asha, null));
        assertThat(trend.size()).isEqualTo(3);
        assertThat(trend.get(1).get("month").asText()).isEqualTo("2026-09");
        assertThat(trend.get(1).get("expense").decimalValue()).isEqualByComparingTo("999.00");
        assertThat(trend.get(2).get("income").decimalValue()).isEqualByComparingTo("80000.00");
    }

    @Test
    void exportsExcelAndPdf() throws Exception {
        createTxn(asha, categoryId(asha, "Groceries"), "450.00", "2026-10-05");
        byte[] xlsx = mvc.perform(get("/api/reports/export?from=2026-10-01&to=2026-10-31&format=xlsx")
                        .header("Authorization", "Bearer " + asha))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(xlsx, 0, 2)).isEqualTo("PK");
        byte[] pdf = mvc.perform(get("/api/reports/export?from=2026-10-01&to=2026-10-31&format=pdf")
                        .header("Authorization", "Bearer " + asha))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        send(get("/api/reports/export?from=2026-10-01&to=2026-10-31&format=doc"), asha, null)
                .andExpect(status().isBadRequest());
    }

    // ---- recurring ----

    @Test
    void recurringRulePostsMissedOccurrencesOnceAndAdvances() throws Exception {
        long rent = categoryId(asha, "Rent");
        JsonNode rule = read(send(post("/api/recurring"), asha,
                "{\"categoryId\":" + rent + ",\"amount\":15000,\"description\":\"House rent\","
                        + "\"frequency\":\"MONTHLY\",\"startDate\":\"2026-07-31\"}"));
        // Jul 31, Aug 31, Sep 30 are on or before today (Oct 14); Oct 31 is next
        assertThat(rule.get("nextRunDate").asText()).isEqualTo("2026-10-31");
        send(get("/api/transactions"), asha, null).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].date").value("2026-09-30"))
                .andExpect(jsonPath("$.content[0].recurring").value(true));

        long id = rule.get("id").asLong();
        send(post("/api/recurring/" + id + "/pause"), asha, null).andExpect(jsonPath("$.active").value(false));
        send(post("/api/recurring/" + id + "/resume"), asha, null)
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.nextRunDate").value("2026-10-31"));
        // resuming never double-posts
        send(get("/api/transactions"), asha, null).andExpect(jsonPath("$.totalElements").value(3));

        // deleting the rule keeps the transactions it already posted
        send(delete("/api/recurring/" + id), asha, null).andExpect(status().isNoContent());
        send(get("/api/transactions"), asha, null).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].recurring").value(false));
    }

    @Test
    void recurringRuleStopsAtEndDate() throws Exception {
        long rent = categoryId(asha, "Rent");
        send(post("/api/recurring"), asha,
                "{\"categoryId\":" + rent + ",\"amount\":100,\"description\":\"Gym\",\"frequency\":\"WEEKLY\","
                        + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-20\"}")
                .andExpect(jsonPath("$.active").value(false));
        // Sep 1, 8, 15
        send(get("/api/transactions"), asha, null).andExpect(jsonPath("$.totalElements").value(3));
        send(post("/api/recurring"), asha,
                "{\"categoryId\":" + rent + ",\"amount\":100,\"description\":\"Bad\",\"frequency\":\"WEEKLY\","
                        + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-08-01\"}")
                .andExpect(status().isBadRequest());
    }

    // ---- helpers ----

    String register(String name) throws Exception {
        String body = body("email", emailOf(name), "password", "password123", "displayName", name);
        // every test gets fresh users, so emails are unique per test run
        return read(send(post("/api/auth/register"), null, body).andExpect(status().isCreated()))
                .get("token").asText();
    }

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    String emailOf(String name) {
        return name + "-" + suffix + "@example.com";
    }

    long categoryId(String token, String name) throws Exception {
        for (JsonNode c : read(send(get("/api/categories"), token, null))) {
            if (c.get("name").asText().equals(name)) {
                return c.get("id").asLong();
            }
        }
        throw new AssertionError("No category " + name);
    }

    long createTxn(String token, long categoryId, String amount, String date) throws Exception {
        return read(send(post("/api/transactions"), token, txnBody(categoryId, amount, date))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    long createRule(String token, long categoryId, String start) throws Exception {
        return read(send(post("/api/recurring"), token, "{\"categoryId\":" + categoryId
                + ",\"amount\":10,\"description\":\"Rule\",\"frequency\":\"MONTHLY\",\"startDate\":\"" + start + "\"}")
                .andExpect(status().isCreated())).get("id").asLong();
    }

    static String txnBody(long categoryId, String amount, String date) {
        return "{\"categoryId\":" + categoryId + ",\"amount\":" + amount + ",\"date\":\"" + date + "\"}";
    }

    String body(String... kv) {
        var node = json.createObjectNode();
        for (int i = 0; i < kv.length; i += 2) {
            node.put(kv[i], kv[i + 1]);
        }
        return node.toString();
    }

    ResultActions send(MockHttpServletRequestBuilder req, String token, String body) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(req);
    }

    JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
