package com.syfe.pfm.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Response bodies of the report endpoints. */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record MonthlyReportResponse(int month, int year, Map<String, BigDecimal> totalIncome,
                                        Map<String, BigDecimal> totalExpenses, BigDecimal netSavings) {
    }

    public record YearlyReportResponse(int year, Map<String, BigDecimal> totalIncome,
                                       Map<String, BigDecimal> totalExpenses, BigDecimal netSavings) {
    }
}
