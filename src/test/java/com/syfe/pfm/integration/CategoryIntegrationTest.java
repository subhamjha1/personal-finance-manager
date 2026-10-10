package com.syfe.pfm.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CategoryIntegrationTest extends AbstractIntegrationTest {

    private Set<String> names(JsonNode listResponse) {
        Set<String> names = new HashSet<>();
        listResponse.get("categories").forEach(c -> names.add(c.get("name").asText()));
        return names;
    }

    @Test
    void list_containsSevenDefaultsMarkedNotCustom() throws Exception {
        TestUser user = newUser();
        JsonNode body = call(user.session(), get("/api/categories"), null, 200);
        assertEquals(7, body.get("categories").size());
        assertEquals(Set.of("Salary", "Food", "Rent", "Transportation", "Entertainment", "Healthcare", "Utilities"),
                names(body));
        body.get("categories").forEach(c -> assertFalse(c.get("isCustom").asBoolean()));
        body.get("categories").forEach(c -> {
            String expected = c.get("name").asText().equals("Salary") ? "INCOME" : "EXPENSE";
            assertEquals(expected, c.get("type").asText());
        });
    }

    @Test
    void create_customCategory_returns201AndAppearsInList() throws Exception {
        TestUser user = newUser();
        JsonNode created = call(user.session(), post("/api/categories"),
                Map.of("name", "SideBusinessIncome", "type", "INCOME"), 201);
        assertEquals("SideBusinessIncome", created.get("name").asText());
        assertEquals("INCOME", created.get("type").asText());
        assertTrue(created.get("isCustom").asBoolean());

        JsonNode list = call(user.session(), get("/api/categories"), null, 200);
        assertTrue(names(list).contains("SideBusinessIncome"));
        assertEquals(8, list.get("categories").size());
    }

    @Test
    void create_duplicateName_returns409_includingCaseVariantsAndDefaultNames() throws Exception {
        TestUser user = newUser();
        call(user.session(), post("/api/categories"), Map.of("name", "Gifts", "type", "EXPENSE"), 201);
        call(user.session(), post("/api/categories"), Map.of("name", "Gifts", "type", "EXPENSE"), 409);
        call(user.session(), post("/api/categories"), Map.of("name", "gifts", "type", "INCOME"), 409);
        call(user.session(), post("/api/categories"), Map.of("name", "Food", "type", "EXPENSE"), 409);
    }

    @Test
    void create_invalidInput_returns400() throws Exception {
        TestUser user = newUser();
        call(user.session(), post("/api/categories"), Map.of("name", "Bad", "type", "SAVINGS"), 400);
        call(user.session(), post("/api/categories"), Map.of("type", "INCOME"), 400);
        call(user.session(), post("/api/categories"), Map.of("name", "NoType"), 400);
        call(user.session(), post("/api/categories"), Map.of("name", "   ", "type", "INCOME"), 400);
    }

    @Test
    void delete_customCategory_returns200AndRemovesIt() throws Exception {
        TestUser user = newUser();
        call(user.session(), post("/api/categories"), Map.of("name", "My Category", "type", "EXPENSE"), 201);
        JsonNode body = call(user.session(), delete("/api/categories/{name}", "My Category"), null, 200);
        assertEquals("Category deleted successfully", body.get("message").asText());
        JsonNode list = call(user.session(), get("/api/categories"), null, 200);
        assertFalse(names(list).contains("My Category"));
    }

    @Test
    void delete_defaultCategory_returns403() throws Exception {
        TestUser user = newUser();
        call(user.session(), delete("/api/categories/{name}", "Salary"), null, 403);
        call(user.session(), delete("/api/categories/{name}", "food"), null, 403);
    }

    @Test
    void delete_unknownCategory_returns404() throws Exception {
        TestUser user = newUser();
        call(user.session(), delete("/api/categories/{name}", "Nope"), null, 404);
    }

    @Test
    void delete_categoryReferencedByTransaction_returns400_thenSucceedsAfterTransactionDeleted() throws Exception {
        TestUser user = newUser();
        call(user.session(), post("/api/categories"), Map.of("name", "Freelance", "type", "INCOME"), 201);
        long txId = addTransaction(user, "100.00", today(), "Freelance");

        call(user.session(), delete("/api/categories/{name}", "Freelance"), null, 400);

        deleteTransaction(user, txId);
        call(user.session(), delete("/api/categories/{name}", "Freelance"), null, 200);
    }

    @Test
    void deletedCategory_cannotBeUsedForNewTransactions() throws Exception {
        TestUser user = newUser();
        call(user.session(), post("/api/categories"), Map.of("name", "Temp", "type", "EXPENSE"), 201);
        call(user.session(), delete("/api/categories/{name}", "Temp"), null, 200);
        call(user.session(), post("/api/transactions"),
                Map.of("amount", 10, "date", today().toString(), "category", "Temp"), 400);
    }

    @Test
    void customCategories_areIsolatedBetweenUsers() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();
        call(a.session(), post("/api/categories"), Map.of("name", "OnlyA", "type", "EXPENSE"), 201);

        JsonNode listB = call(b.session(), get("/api/categories"), null, 200);
        assertFalse(names(listB).contains("OnlyA"));

        // B cannot delete A's category, and cannot use it for transactions
        call(b.session(), delete("/api/categories/{name}", "OnlyA"), null, 404);
        call(b.session(), post("/api/transactions"),
                Map.of("amount", 10, "date", today().toString(), "category", "OnlyA"), 400);

        // A's category is untouched
        assertTrue(names(call(a.session(), get("/api/categories"), null, 200)).contains("OnlyA"));

        // B may create its own category with the same name
        call(b.session(), post("/api/categories"), Map.of("name", "OnlyA", "type", "INCOME"), 201);
    }
}
