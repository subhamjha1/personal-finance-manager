package com.syfe.pfm.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoalIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> goalBody(String name, Object target, String targetDate, String startDate) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("goalName", name);
        m.put("targetAmount", target);
        m.put("targetDate", targetDate);
        if (startDate != null) {
            m.put("startDate", startDate);
        }
        return m;
    }

    @Test
    void create_defaultsStartDateToToday_andStartsAtZeroProgress() throws Exception {
        TestUser user = newUser();
        JsonNode goal = call(user.session(), post("/api/goals"),
                goalBody("Emergency Fund", new BigDecimal("5000.00"), today().plusDays(100).toString(), null), 201);
        assertEquals("Emergency Fund", goal.get("goalName").asText());
        assertEquals(today().toString(), goal.get("startDate").asText());
        assertEquals(today().plusDays(100).toString(), goal.get("targetDate").asText());
        assertMoney("5000.00", goal.get("targetAmount"));
        assertMoney("0.00", goal.get("currentProgress"));
        assertMoney("0.00", goal.get("progressPercentage"));
        assertMoney("5000.00", goal.get("remainingAmount"));
    }

    @Test
    void progress_isIncomeMinusExpensesSinceStartDate() throws Exception {
        TestUser user = newUser();
        LocalDate start = today().minusDays(30);
        addTransaction(user, "1000.00", today().minusDays(60), "Salary"); // before start: excluded
        addTransaction(user, "3000.00", today().minusDays(10), "Salary");
        addTransaction(user, "500.00", today().minusDays(5), "Food");
        long id = createGoal(user, "5000.00", today().plusDays(100), start);

        JsonNode goal = call(user.session(), get("/api/goals/{id}", id), null, 200);
        assertMoney("2500.00", goal.get("currentProgress"));
        assertMoney("50.00", goal.get("progressPercentage"));
        assertMoney("2500.00", goal.get("remainingAmount"));
    }

    @Test
    void progress_startDateIsInclusive() throws Exception {
        TestUser user = newUser();
        LocalDate start = today().minusDays(3);
        addTransaction(user, "200.00", start, "Salary");
        addTransaction(user, "50.00", start.minusDays(1), "Salary");
        long id = createGoal(user, "1000.00", today().plusDays(10), start);
        assertMoney("200.00", call(user.session(), get("/api/goals/{id}", id), null, 200).get("currentProgress"));
    }

    @Test
    void progress_canBeNegativeOrExceed100Percent_withoutCapping() throws Exception {
        TestUser user = newUser();
        LocalDate start = today().minusDays(10);
        addTransaction(user, "300.00", today().minusDays(2), "Food");
        long id = createGoal(user, "1000.00", today().plusDays(10), start);
        JsonNode negative = call(user.session(), get("/api/goals/{id}", id), null, 200);
        assertMoney("-300.00", negative.get("currentProgress"));
        assertMoney("-30.00", negative.get("progressPercentage"));
        assertMoney("1300.00", negative.get("remainingAmount"));

        addTransaction(user, "5000.00", today().minusDays(1), "Salary");
        JsonNode over = call(user.session(), get("/api/goals/{id}", id), null, 200);
        assertMoney("4700.00", over.get("currentProgress"));
        assertMoney("470.00", over.get("progressPercentage"));
        assertMoney("-3700.00", over.get("remainingAmount"));
    }

    @Test
    void goalsTrackIndependently() throws Exception {
        TestUser user = newUser();
        addTransaction(user, "1000.00", today().minusDays(20), "Salary");
        addTransaction(user, "400.00", today().minusDays(2), "Salary");
        long old = createGoal(user, "2000.00", today().plusDays(50), today().minusDays(30));
        long recent = createGoal(user, "2000.00", today().plusDays(50), today().minusDays(5));
        assertMoney("1400.00", call(user.session(), get("/api/goals/{id}", old), null, 200).get("currentProgress"));
        assertMoney("400.00", call(user.session(), get("/api/goals/{id}", recent), null, 200).get("currentProgress"));
        assertEquals(2, call(user.session(), get("/api/goals"), null, 200).get("goals").size());
    }

    @Test
    void deletingTransaction_immediatelyChangesGoalProgress() throws Exception {
        TestUser user = newUser();
        long tx = addTransaction(user, "700.00", today().minusDays(1), "Salary");
        long id = createGoal(user, "1000.00", today().plusDays(10), today().minusDays(5));
        assertMoney("700.00", call(user.session(), get("/api/goals/{id}", id), null, 200).get("currentProgress"));
        deleteTransaction(user, tx);
        assertMoney("0.00", call(user.session(), get("/api/goals/{id}", id), null, 200).get("currentProgress"));
    }

    @Test
    void goalProgress_ignoresOtherUsersTransactions() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();
        addTransaction(b, "9999.00", today().minusDays(1), "Salary");
        long id = createGoal(a, "1000.00", today().plusDays(10), today().minusDays(5));
        assertMoney("0.00", call(a.session(), get("/api/goals/{id}", id), null, 200).get("currentProgress"));
    }

    @Test
    void create_invalidInput_returns400() throws Exception {
        TestUser user = newUser();
        String future = today().plusDays(10).toString();
        call(user.session(), post("/api/goals"), goalBody(null, 100, future, null), 400);
        call(user.session(), post("/api/goals"), goalBody("  ", 100, future, null), 400);
        call(user.session(), post("/api/goals"), goalBody("G", -100, future, null), 400);
        call(user.session(), post("/api/goals"), goalBody("G", 0, future, null), 400);
        call(user.session(), post("/api/goals"), goalBody("G", null, future, null), 400);
        call(user.session(), post("/api/goals"), goalBody("G", 100, null, null), 400);
        call(user.session(), post("/api/goals"), goalBody("G", 100, today().minusDays(1).toString(), null), 400);
        call(user.session(), post("/api/goals"), goalBody("G", 100, today().toString(), null), 400);
        call(user.session(), post("/api/goals"), goalBody("G", 100, "not-a-date", null), 400);
        // target date must be after the start date
        call(user.session(), post("/api/goals"),
                goalBody("G", 100, future, today().plusDays(20).toString()), 400);
    }

    @Test
    void update_partial_preservesOtherFields() throws Exception {
        TestUser user = newUser();
        addTransaction(user, "1000.00", today().minusDays(1), "Salary");
        long id = createGoal(user, "5000.00", today().plusDays(100), today().minusDays(5));
        String newDate = today().plusDays(200).toString();
        JsonNode updated = call(user.session(), put("/api/goals/{id}", id),
                Map.of("targetAmount", new BigDecimal("6000.00"), "targetDate", newDate), 200);
        assertEquals("Emergency Fund", updated.get("goalName").asText());
        assertEquals(today().minusDays(5).toString(), updated.get("startDate").asText());
        assertEquals(newDate, updated.get("targetDate").asText());
        assertMoney("6000.00", updated.get("targetAmount"));
        assertMoney("1000.00", updated.get("currentProgress"));
        assertMoney("16.67", updated.get("progressPercentage"));
        assertMoney("5000.00", updated.get("remainingAmount"));

        JsonNode renamed = call(user.session(), put("/api/goals/{id}", id), Map.of("goalName", "Renamed"), 200);
        assertEquals("Renamed", renamed.get("goalName").asText());
        assertMoney("6000.00", renamed.get("targetAmount"));
    }

    @Test
    void update_invalidValues_return400() throws Exception {
        TestUser user = newUser();
        long id = createGoal(user, "5000.00", today().plusDays(100), null);
        call(user.session(), put("/api/goals/{id}", id), Map.of("targetAmount", -1), 400);
        call(user.session(), put("/api/goals/{id}", id), Map.of("targetDate", today().minusDays(1).toString()), 400);
        call(user.session(), put("/api/goals/{id}", id), Map.of("goalName", "   "), 400);
    }

    @Test
    void delete_removesGoal() throws Exception {
        TestUser user = newUser();
        long id = createGoal(user, "5000.00", today().plusDays(100), null);
        JsonNode body = call(user.session(), delete("/api/goals/{id}", id), null, 200);
        assertEquals("Goal deleted successfully", body.get("message").asText());
        call(user.session(), get("/api/goals/{id}", id), null, 404);
    }

    @Test
    void nonexistentAndMalformedIds() throws Exception {
        TestUser user = newUser();
        call(user.session(), get("/api/goals/{id}", 999999999L), null, 404);
        call(user.session(), put("/api/goals/{id}", 999999999L), Map.of("targetAmount", 5), 404);
        call(user.session(), delete("/api/goals/{id}", 999999999L), null, 404);
        call(user.session(), get("/api/goals/{id}", "abc"), null, 400);
    }

    @Test
    void userIsolation_otherUsersGoalsAreForbidden() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();
        long id = createGoal(a, "5000.00", today().plusDays(100), null);

        call(b.session(), get("/api/goals/{id}", id), null, 403);
        call(b.session(), put("/api/goals/{id}", id), Map.of("targetAmount", 1), 403);
        call(b.session(), delete("/api/goals/{id}", id), null, 403);
        assertEquals(0, call(b.session(), get("/api/goals"), null, 200).get("goals").size());

        JsonNode goal = call(a.session(), get("/api/goals/{id}", id), null, 200);
        assertMoney("5000.00", goal.get("targetAmount"));
    }
}
