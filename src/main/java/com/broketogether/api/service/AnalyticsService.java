package com.broketogether.api.service;

import com.broketogether.api.dto.AnalyticsResponse;
import com.broketogether.api.model.Expense;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.utility.Utility;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService extends Utility {

    private final ExpenseRepository expenseRepository;
    private final HomeRepository homeRepository;

    public AnalyticsService(ExpenseRepository expenseRepository, HomeRepository homeRepository) {
        this.expenseRepository = expenseRepository;
        this.homeRepository = homeRepository;
    }

    public AnalyticsResponse getAnalytics(Long homeId) throws AccountNotFoundException {
        User user = getUserDetails();

        if (!user.getPremium()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Upgrade to Premium to access analytics.");
        }

        Home home = homeRepository.findById(homeId)
            .orElseThrow(() -> new RuntimeException("Home not found."));

        boolean isMember = home.getMembers().stream()
            .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not a member of this home.");
        }

        List<Expense> allExpenses = expenseRepository.findByHomeId(homeId);

        // Settlements paid by current user
        BigDecimal totalSettlements = allExpenses.stream()
            .filter(e -> "SETTLEMENT".equalsIgnoreCase(e.getCategory()))
            .filter(e -> e.getPayer() != null && e.getPayer().getId().equals(user.getId()))
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Real expenses only
        List<Expense> realExpenses = allExpenses.stream()
            .filter(e -> !"SETTLEMENT".equalsIgnoreCase(e.getCategory()))
            .collect(Collectors.toList());

        // Current user's own payments
        List<Expense> myExpenses = realExpenses.stream()
            .filter(e -> e.getPayer() != null && e.getPayer().getId().equals(user.getId()))
            .collect(Collectors.toList());

        // ── B8 fix 1: category shares as percentages that sum to exactly 100 ──
        BigDecimal myTotal = myExpenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> rawByCategory = myExpenses.stream()
            .collect(Collectors.groupingBy(
                e -> e.getCategory() != null ? e.getCategory() : "General",
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
            ));

        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        if (myTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal running = BigDecimal.ZERO;
            List<Map.Entry<String, BigDecimal>> sorted = rawByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

            for (int i = 0; i < sorted.size(); i++) {
                Map.Entry<String, BigDecimal> entry = sorted.get(i);
                BigDecimal pct;
                if (i == sorted.size() - 1) {
                    // Last category gets the remainder to guarantee sum = 100
                    pct = new BigDecimal("100").subtract(running);
                } else {
                    pct = entry.getValue()
                        .multiply(new BigDecimal("100"))
                        .divide(myTotal, 2, RoundingMode.HALF_UP);
                    running = running.add(pct);
                }
                byCategory.put(entry.getKey(), pct);
            }
        }

        // ── B8 fix 2: monthly series with no gaps (last 6 months) ──────────────
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        Map<String, BigDecimal> rawMonthly = myExpenses.stream()
            .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(sixMonthsAgo))
            .collect(Collectors.groupingBy(
                e -> e.getCreatedAt().getYear() + "-" +
                     String.format("%02d", e.getCreatedAt().getMonthValue()),
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
            ));

        // Fill in every month in the range with 0 if no expenses
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            String key = cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue());
            monthly.put(key, rawMonthly.getOrDefault(key, BigDecimal.ZERO));
            cursor = cursor.plusMonths(1);
        }

        // ── B8 fix 3: by-member includes ALL nonzero contributors ─────────────
        Map<String, BigDecimal> byMember = realExpenses.stream()
            .filter(e -> e.getPayer() != null)
            .collect(Collectors.groupingBy(
                e -> e.getPayer().getName(),
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
            ))
            .entrySet().stream()
            .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (a, b) -> a, LinkedHashMap::new));

        Expense largest = myExpenses.stream()
            .max(Comparator.comparing(Expense::getAmount))
            .orElse(null);

        return new AnalyticsResponse(
            byCategory,
            monthly,
            byMember,
            largest != null ? largest.getAmount() : BigDecimal.ZERO,
            largest != null ? largest.getDescription() : "N/A",
            totalSettlements
        );
    }
}
