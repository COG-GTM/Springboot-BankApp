package com.example.bankapp.controller;

import com.example.bankapp.dto.CategorySpendingDto;
import com.example.bankapp.dto.DailyTrendDto;
import com.example.bankapp.dto.DateRangeRequest;
import com.example.bankapp.dto.MonthlySummaryDto;
import com.example.bankapp.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    static final long MAX_RANGE_DAYS = 366;

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/spending-by-category")
    public ResponseEntity<List<CategorySpendingDto>> spendingByCategory(
            @Valid @ModelAttribute DateRangeRequest range,
            Authentication authentication) {
        validateRange(range);
        return ResponseEntity.ok(analyticsService.getSpendingByCategory(
                currentUsername(authentication), range.getStartDate(), range.getEndDate()));
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<List<MonthlySummaryDto>> monthlySummary(Authentication authentication) {
        return ResponseEntity.ok(analyticsService.getMonthlySummary(currentUsername(authentication)));
    }

    @GetMapping("/trends")
    public ResponseEntity<List<DailyTrendDto>> trends(
            @Valid @ModelAttribute DateRangeRequest range,
            Authentication authentication) {
        validateRange(range);
        return ResponseEntity.ok(analyticsService.getTrends(
                currentUsername(authentication), range.getStartDate(), range.getEndDate()));
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        return authentication.getName();
    }

    private void validateRange(DateRangeRequest range) {
        if (range.getStartDate().isAfter(range.getEndDate())) {
            throw new ResponseStatusException(BAD_REQUEST, "startDate must be on or before endDate");
        }
        long days = ChronoUnit.DAYS.between(range.getStartDate(), range.getEndDate());
        if (days > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
    }
}
