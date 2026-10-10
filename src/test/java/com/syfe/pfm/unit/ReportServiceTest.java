package com.syfe.pfm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.syfe.pfm.dto.ReportDtos.MonthlyReportResponse;
import com.syfe.pfm.entity.CategoryType;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.repository.TransactionRepository;
import com.syfe.pfm.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @InjectMocks
    private ReportService reportService;

    @Test
    void monthly_usesInclusiveCalendarBoundsAndComputesNet() {
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 2, 29);
        when(transactionRepository.sumByCategory(1L, CategoryType.INCOME, from, to))
                .thenReturn(List.<Object[]>of(new Object[]{"Salary", new BigDecimal("3000")}));
        when(transactionRepository.sumByCategory(1L, CategoryType.EXPENSE, from, to))
                .thenReturn(List.<Object[]>of(new Object[]{"Food", new BigDecimal("400.50")},
                        new Object[]{"Rent", new BigDecimal("1200")}));

        MonthlyReportResponse report = reportService.monthly(1L, 2024, 2);

        assertEquals(2, report.month());
        assertEquals(2024, report.year());
        assertEquals(0, new BigDecimal("3000.00").compareTo(report.totalIncome().get("Salary")));
        assertEquals(2, report.totalExpenses().size());
        assertEquals(0, new BigDecimal("1399.50").compareTo(report.netSavings()));
    }

    @Test
    void yearly_withoutData_returnsZeroNet() {
        when(transactionRepository.sumByCategory(eq(1L), any(CategoryType.class), any(LocalDate.class),
                any(LocalDate.class))).thenReturn(List.of());
        assertEquals(0, BigDecimal.ZERO.compareTo(reportService.yearly(1L, 2024).netSavings()));
    }

    @Test
    void invalidMonthOrYear_throwsBadRequest() {
        assertThrows(ApiException.class, () -> reportService.monthly(1L, 2024, 13));
        assertThrows(ApiException.class, () -> reportService.monthly(1L, 2024, 0));
        assertThrows(ApiException.class, () -> reportService.monthly(1L, 0, 5));
        assertThrows(ApiException.class, () -> reportService.yearly(1L, 10000));
    }
}
