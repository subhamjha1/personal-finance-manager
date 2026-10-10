package com.syfe.pfm.controller;

import com.syfe.pfm.dto.ReportDtos.MonthlyReportResponse;
import com.syfe.pfm.dto.ReportDtos.YearlyReportResponse;
import com.syfe.pfm.security.UserPrincipal;
import com.syfe.pfm.service.ReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Monthly and yearly report endpoints. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly/{year}/{month}")
    public MonthlyReportResponse monthly(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable int year, @PathVariable int month) {
        return reportService.monthly(principal.getId(), year, month);
    }

    @GetMapping("/yearly/{year}")
    public YearlyReportResponse yearly(@AuthenticationPrincipal UserPrincipal principal, @PathVariable int year) {
        return reportService.yearly(principal.getId(), year);
    }
}
