package com.syfe.pfm.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportIntegrationTest extends AbstractIntegrationTest {

    private TestUser userWithData() throws Exception {
        TestUser user = newUser();
        call(user.session(), post("/api/categories"), Map.of("name", "Freelance", "type", "INCOME"), 201);
        addTransaction(user, "3000.00", LocalDate.of(2023, 3, 1), "Salary");
        addTransaction(user, "500.00", LocalDate.of(2023, 3, 31), "Freelance");
        addTransaction(user, "400.00", LocalDate.of(2023, 3, 15), "Food");
        addTransaction(user, "1200.00", LocalDate.of(2023, 3, 20), "Rent");
        addTransaction(user, "100.00", LocalDate.of(2023, 4, 1), "Food");      // next month
        addTransaction(user, "999.00", LocalDate.of(2022, 12, 31), "Salary");  // previous year
        return user;
    }

    @Test
    void monthly_aggregatesByCategoryWithinCalendarMonth() throws Exception {
        TestUser user = userWithData();
        JsonNode report = call(user.session(), get("/api/reports/monthly/{y}/{m}", 2023, 3), null, 200);
        assertEquals(3, report.get("month").asInt());
        assertEquals(2023, report.get("year").asInt());
        assertEquals(2, report.get("totalIncome").size());
        assertMoney("3000.00", report.get("totalIncome").get("Salary"));
        assertMoney("500.00", report.get("totalIncome").get("Freelance"));
        assertEquals(2, report.get("totalExpenses").size());
        assertMoney("400.00", report.get("totalExpenses").get("Food"));
        assertMoney("1200.00", report.get("totalExpenses").get("Rent"));
        assertMoney("1900.00", report.get("netSavings"));
    }

    @Test
    void yearly_aggregatesWholeYear() throws Exception {
        TestUser user = userWithData();
        JsonNode report = call(user.session(), get("/api/reports/yearly/{y}", 2023), null, 200);
        assertEquals(2023, report.get("year").asInt());
        assertMoney("3000.00", report.get("totalIncome").get("Salary"));
        assertMoney("500.00", report.get("totalIncome").get("Freelance"));
        assertMoney("500.00", report.get("totalExpenses").get("Food"));
        assertMoney("1200.00", report.get("totalExpenses").get("Rent"));
        assertMoney("1800.00", report.get("netSavings"));
    }

    @Test
    void reports_withNoTransactions_areEmptyWithZeroNet() throws Exception {
        TestUser user = newUser();
        JsonNode monthly = call(user.session(), get("/api/reports/monthly/{y}/{m}", 2020, 2), null, 200);
        assertEquals(0, monthly.get("totalIncome").size());
        assertEquals(0, monthly.get("totalExpenses").size());
        assertMoney("0.00", monthly.get("netSavings"));
        JsonNode yearly = call(user.session(), get("/api/reports/yearly/{y}", 2020), null, 200);
        assertEquals(0, yearly.get("totalIncome").size());
        assertMoney("0.00", yearly.get("netSavings"));
    }

    @Test
    void reports_handleLeapDayAndYearEndBoundaries() throws Exception {
        TestUser user = newUser();
        addTransaction(user, "10.00", LocalDate.of(2024, 2, 29), "Food");
        addTransaction(user, "20.00", LocalDate.of(2024, 12, 31), "Food");
        addTransaction(user, "40.00", LocalDate.of(2024, 1, 1), "Food");
        JsonNode feb = call(user.session(), get("/api/reports/monthly/{y}/{m}", 2024, 2), null, 200);
        assertMoney("10.00", feb.get("totalExpenses").get("Food"));
        JsonNode dec = call(user.session(), get("/api/reports/monthly/{y}/{m}", 2024, 12), null, 200);
        assertMoney("20.00", dec.get("totalExpenses").get("Food"));
        JsonNode year = call(user.session(), get("/api/reports/yearly/{y}", 2024), null, 200);
        assertMoney("70.00", year.get("totalExpenses").get("Food"));
        assertMoney("-70.00", year.get("netSavings"));
    }

    @Test
    void reports_excludeDeletedTransactions() throws Exception {
        TestUser user = newUser();
        long keep = addTransaction(user, "100.00", LocalDate.of(2023, 5, 10), "Food");
        long gone = addTransaction(user, "50.00", LocalDate.of(2023, 5, 11), "Food");
        deleteTransaction(user, gone);
        JsonNode report = call(user.session(), get("/api/reports/monthly/{y}/{m}", 2023, 5), null, 200);
        assertMoney("100.00", report.get("totalExpenses").get("Food"));
        assertTrue(keep > 0);
        deleteTransaction(user, keep);
        JsonNode empty = call(user.session(), get("/api/reports/yearly/{y}", 2023), null, 200);
        assertEquals(0, empty.get("totalExpenses").size());
    }

    @Test
    void reports_containOnlyTheAuthenticatedUsersData() throws Exception {
        TestUser a = userWithData();
        TestUser b = newUser();
        addTransaction(b, "7.00", LocalDate.of(2023, 3, 2), "Food");
        JsonNode reportB = call(b.session(), get("/api/reports/monthly/{y}/{m}", 2023, 3), null, 200);
        assertEquals(0, reportB.get("totalIncome").size());
        assertMoney("7.00", reportB.get("totalExpenses").get("Food"));
        assertMoney("-7.00", reportB.get("netSavings"));
        JsonNode reportA = call(a.session(), get("/api/reports/monthly/{y}/{m}", 2023, 3), null, 200);
        assertMoney("400.00", reportA.get("totalExpenses").get("Food"));
    }

    @Test
    void invalidPeriods_return400() throws Exception {
        TestUser user = newUser();
        call(user.session(), get("/api/reports/monthly/{y}/{m}", 2024, 0), null, 400);
        call(user.session(), get("/api/reports/monthly/{y}/{m}", 2024, 13), null, 400);
        call(user.session(), get("/api/reports/monthly/{y}/{m}", 0, 5), null, 400);
        call(user.session(), get("/api/reports/yearly/{y}", 0), null, 400);
        call(user.session(), get("/api/reports/yearly/{y}", 10000), null, 400);
        call(user.session(), get("/api/reports/monthly/{y}/{m}", "abc", 5), null, 400);
        call(user.session(), get("/api/reports/yearly/{y}", "abc"), null, 400);
    }
}
