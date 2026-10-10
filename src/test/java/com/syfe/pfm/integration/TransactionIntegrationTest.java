package com.syfe.pfm.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransactionIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> body(Object amount, String date, String category) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("amount", amount);
        m.put("date", date);
        m.put("category", category);
        return m;
    }

    @Test
    void create_returns201WithDerivedType() throws Exception {
        TestUser user = newUser();
        Map<String, Object> request = body(new BigDecimal("50000.00"), "2024-01-15", "Salary");
        request.put("description", "January Salary");
        JsonNode tx = call(user.session(), post("/api/transactions"), request, 201);
        assertTrue(tx.get("id").asLong() > 0);
        assertMoney("50000.00", tx.get("amount"));
        assertEquals("2024-01-15", tx.get("date").asText());
        assertEquals("Salary", tx.get("category").asText());
        assertEquals("January Salary", tx.get("description").asText());
        assertEquals("INCOME", tx.get("type").asText());

        JsonNode expense = call(user.session(), post("/api/transactions"),
                body(new BigDecimal("20.5"), "2024-01-16", "Food"), 201);
        assertEquals("EXPENSE", expense.get("type").asText());
        assertTrue(expense.get("description").isNull());
    }

    @Test
    void create_ignoresClientSuppliedType() throws Exception {
        TestUser user = newUser();
        Map<String, Object> request = body(new BigDecimal("10.00"), "2024-01-15", "Food");
        request.put("type", "INCOME");
        JsonNode tx = call(user.session(), post("/api/transactions"), request, 201);
        assertEquals("EXPENSE", tx.get("type").asText());
    }

    @Test
    void create_categoryLookupIsCaseInsensitive() throws Exception {
        TestUser user = newUser();
        JsonNode tx = call(user.session(), post("/api/transactions"),
                body(new BigDecimal("10.00"), "2024-01-15", "salary"), 201);
        assertEquals("Salary", tx.get("category").asText());
    }

    @Test
    void create_invalidInput_returns400() throws Exception {
        TestUser user = newUser();
        call(user.session(), post("/api/transactions"), body(new BigDecimal("-5"), "2024-01-15", "Food"), 400);
        call(user.session(), post("/api/transactions"), body(BigDecimal.ZERO, "2024-01-15", "Food"), 400);
        call(user.session(), post("/api/transactions"), body("abc", "2024-01-15", "Food"), 400);
        call(user.session(), post("/api/transactions"), body(new BigDecimal("1.234"), "2024-01-15", "Food"), 400);
        call(user.session(), post("/api/transactions"), body(null, "2024-01-15", "Food"), 400);
        call(user.session(), post("/api/transactions"), body(new BigDecimal("5"), null, "Food"), 400);
        call(user.session(), post("/api/transactions"), body(new BigDecimal("5"), "2024-13-45", "Food"), 400);
        call(user.session(), post("/api/transactions"), body(new BigDecimal("5"), "15/01/2024", "Food"), 400);
        call(user.session(), post("/api/transactions"), body(new BigDecimal("5"), "2024-01-15", null), 400);
        call(user.session(), post("/api/transactions"), body(new BigDecimal("5"), "2024-01-15", "NoSuchCategory"), 400);
        call(user.session(), post("/api/transactions"), "{ bad json", 400);
    }

    @Test
    void create_futureDate_returns400_butTodayIsAllowed() throws Exception {
        TestUser user = newUser();
        call(user.session(), post("/api/transactions"),
                body(new BigDecimal("5"), today().plusDays(1).toString(), "Food"), 400);
        call(user.session(), post("/api/transactions"),
                body(new BigDecimal("5"), today().toString(), "Food"), 201);
    }

    @Test
    void list_isSortedNewestFirst() throws Exception {
        TestUser user = newUser();
        addTransaction(user, "1.00", today().minusDays(3), "Food");
        addTransaction(user, "2.00", today().minusDays(1), "Food");
        addTransaction(user, "3.00", today().minusDays(2), "Food");
        JsonNode list = call(user.session(), get("/api/transactions"), null, 200).get("transactions");
        assertEquals(3, list.size());
        assertEquals(today().minusDays(1).toString(), list.get(0).get("date").asText());
        assertEquals(today().minusDays(2).toString(), list.get(1).get("date").asText());
        assertEquals(today().minusDays(3).toString(), list.get(2).get("date").asText());
    }

    @Test
    void list_emptyForNewUser() throws Exception {
        TestUser user = newUser();
        JsonNode body = call(user.session(), get("/api/transactions"), null, 200);
        assertEquals(0, body.get("transactions").size());
    }

    @Test
    void list_filtersByDateRange_inclusive() throws Exception {
        TestUser user = newUser();
        LocalDate d1 = today().minusDays(10);
        LocalDate d2 = today().minusDays(5);
        LocalDate d3 = today().minusDays(1);
        addTransaction(user, "1.00", d1, "Food");
        addTransaction(user, "2.00", d2, "Food");
        addTransaction(user, "3.00", d3, "Food");

        JsonNode range = call(user.session(),
                get("/api/transactions").param("startDate", d2.toString()).param("endDate", d3.toString()),
                null, 200).get("transactions");
        assertEquals(2, range.size());

        JsonNode fromOnly = call(user.session(),
                get("/api/transactions").param("startDate", d3.toString()), null, 200).get("transactions");
        assertEquals(1, fromOnly.size());

        JsonNode toOnly = call(user.session(),
                get("/api/transactions").param("endDate", d1.toString()), null, 200).get("transactions");
        assertEquals(1, toOnly.size());
    }

    @Test
    void list_filtersByCategoryIdNameAndType() throws Exception {
        TestUser user = newUser();
        addTransaction(user, "100.00", today(), "Salary");
        addTransaction(user, "10.00", today(), "Food");
        addTransaction(user, "20.00", today(), "Rent");

        Long foodId = categoryRepository.findVisibleByName("Food", user.id()).orElseThrow().getId();
        JsonNode byId = call(user.session(), get("/api/transactions").param("categoryId", foodId.toString()),
                null, 200).get("transactions");
        assertEquals(1, byId.size());
        assertEquals("Food", byId.get(0).get("category").asText());

        JsonNode byName = call(user.session(), get("/api/transactions").param("category", "rent"),
                null, 200).get("transactions");
        assertEquals(1, byName.size());

        JsonNode income = call(user.session(), get("/api/transactions").param("type", "income"),
                null, 200).get("transactions");
        assertEquals(1, income.size());
        assertEquals("INCOME", income.get(0).get("type").asText());

        JsonNode expense = call(user.session(), get("/api/transactions").param("type", "EXPENSE"),
                null, 200).get("transactions");
        assertEquals(2, expense.size());
    }

    @Test
    void list_invalidFilters_return400() throws Exception {
        TestUser user = newUser();
        call(user.session(), get("/api/transactions").param("startDate", "garbage"), null, 400);
        call(user.session(), get("/api/transactions").param("endDate", "2024-99-99"), null, 400);
        call(user.session(), get("/api/transactions").param("categoryId", "abc"), null, 400);
        call(user.session(), get("/api/transactions").param("type", "BOGUS"), null, 400);
        call(user.session(), get("/api/transactions")
                .param("startDate", "2024-02-01").param("endDate", "2024-01-01"), null, 400);
    }

    @Test
    void update_amountAndDescription_preservesDateCategoryAndType() throws Exception {
        TestUser user = newUser();
        long id = addTransaction(user, "50000.00", LocalDate.of(2024, 1, 15), "Salary");
        JsonNode updated = call(user.session(), put("/api/transactions/{id}", id),
                Map.of("amount", new BigDecimal("60000.00"), "description", "Updated January Salary"), 200);
        assertEquals(id, updated.get("id").asLong());
        assertMoney("60000.00", updated.get("amount"));
        assertEquals("2024-01-15", updated.get("date").asText());
        assertEquals("Salary", updated.get("category").asText());
        assertEquals("Updated January Salary", updated.get("description").asText());
        assertEquals("INCOME", updated.get("type").asText());
    }

    @Test
    void update_dateChange_isRejected_butSameDateIsAccepted() throws Exception {
        TestUser user = newUser();
        long id = addTransaction(user, "10.00", LocalDate.of(2024, 1, 15), "Food");
        call(user.session(), put("/api/transactions/{id}", id), Map.of("date", "2024-02-01"), 400);
        JsonNode same = call(user.session(), put("/api/transactions/{id}", id),
                Map.of("date", "2024-01-15", "amount", 11), 200);
        assertEquals("2024-01-15", same.get("date").asText());
        // verify unchanged in storage
        JsonNode list = call(user.session(), get("/api/transactions"), null, 200).get("transactions");
        assertEquals("2024-01-15", list.get(0).get("date").asText());
    }

    @Test
    void update_category_changesTypeAndValidatesCategory() throws Exception {
        TestUser user = newUser();
        long id = addTransaction(user, "10.00", LocalDate.of(2024, 1, 15), "Food");
        JsonNode updated = call(user.session(), put("/api/transactions/{id}", id), Map.of("category", "Salary"), 200);
        assertEquals("INCOME", updated.get("type").asText());
        call(user.session(), put("/api/transactions/{id}", id), Map.of("category", "Missing"), 400);
        call(user.session(), put("/api/transactions/{id}", id), Map.of("category", "  "), 400);
    }

    @Test
    void update_invalidAmount_returns400() throws Exception {
        TestUser user = newUser();
        long id = addTransaction(user, "10.00", LocalDate.of(2024, 1, 15), "Food");
        call(user.session(), put("/api/transactions/{id}", id), Map.of("amount", -1), 400);
        call(user.session(), put("/api/transactions/{id}", id), Map.of("amount", 0), 400);
    }

    @Test
    void update_nonexistent_returns404_andMalformedIdReturns400() throws Exception {
        TestUser user = newUser();
        call(user.session(), put("/api/transactions/{id}", 999999999L), Map.of("amount", 5), 404);
        call(user.session(), put("/api/transactions/{id}", "abc"), Map.of("amount", 5), 400);
    }

    @Test
    void delete_removesTransaction_andSecondDeleteReturns404() throws Exception {
        TestUser user = newUser();
        long id = addTransaction(user, "10.00", today(), "Food");
        JsonNode body = call(user.session(), delete("/api/transactions/{id}", id), null, 200);
        assertEquals("Transaction deleted successfully", body.get("message").asText());
        assertEquals(0, call(user.session(), get("/api/transactions"), null, 200).get("transactions").size());
        call(user.session(), delete("/api/transactions/{id}", id), null, 404);
    }

    @Test
    void userIsolation_otherUsersTransactionsAreInvisibleAndImmutable() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();
        long id = addTransaction(a, "10.00", today(), "Food");

        assertEquals(0, call(b.session(), get("/api/transactions"), null, 200).get("transactions").size());
        call(b.session(), put("/api/transactions/{id}", id), Map.of("amount", 99), 404);
        call(b.session(), delete("/api/transactions/{id}", id), null, 404);

        JsonNode listA = call(a.session(), get("/api/transactions"), null, 200).get("transactions");
        assertEquals(1, listA.size());
        assertMoney("10.00", listA.get(0).get("amount"));
    }
}
