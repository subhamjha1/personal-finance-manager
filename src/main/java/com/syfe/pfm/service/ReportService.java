package com.syfe.pfm.service;

import com.syfe.pfm.dto.ReportDtos.MonthlyReportResponse;
import com.syfe.pfm.dto.ReportDtos.YearlyReportResponse;
import com.syfe.pfm.entity.CategoryType;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monthly/yearly aggregation over the user's persisted transactions (inclusive calendar boundaries). */
@Service
public class ReportService {

    static final int MIN_YEAR = 1;
    static final int MAX_YEAR = 9999;

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse monthly(Long userId, int year, int month) {
        validateYear(year);
        if (month < 1 || month > 12) {
            throw ApiException.badRequest("month must be between 1 and 12");
        }
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        Map<String, BigDecimal> income = totals(userId, CategoryType.INCOME, from, to);
        Map<String, BigDecimal> expenses = totals(userId, CategoryType.EXPENSE, from, to);
        return new MonthlyReportResponse(month, year, income, expenses, net(income, expenses));
    }

    @Transactional(readOnly = true)
    public YearlyReportResponse yearly(Long userId, int year) {
        validateYear(year);
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        Map<String, BigDecimal> income = totals(userId, CategoryType.INCOME, from, to);
        Map<String, BigDecimal> expenses = totals(userId, CategoryType.EXPENSE, from, to);
        return new YearlyReportResponse(year, income, expenses, net(income, expenses));
    }

    private Map<String, BigDecimal> totals(Long userId, CategoryType type, LocalDate from, LocalDate to) {
        Map<String, BigDecimal> result = new TreeMap<>();
        for (Object[] row : transactionRepository.sumByCategory(userId, type, from, to)) {
            result.put((String) row[0], ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP));
        }
        return result;
    }

    private static BigDecimal net(Map<String, BigDecimal> income, Map<String, BigDecimal> expenses) {
        BigDecimal in = income.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal out = expenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return in.subtract(out).setScale(2, RoundingMode.HALF_UP);
    }

    private static void validateYear(int year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw ApiException.badRequest("year must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
    }
}
