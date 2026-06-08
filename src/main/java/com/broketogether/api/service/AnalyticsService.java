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
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

        // Settlements paid by current user (money they paid back to others)
        BigDecimal totalSettlements = allExpenses.stream()
            .filter(e -> "SETTLEMENT".equalsIgnoreCase(e.getCategory()))
            .filter(e -> e.getPayer() != null && e.getPayer().getId().equals(user.getId()))
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Real expenses only (no settlements) for category/monthly breakdowns
        List<Expense> realExpenses = allExpenses.stream()
            .filter(e -> !"SETTLEMENT".equalsIgnoreCase(e.getCategory()))
            .collect(Collectors.toList());

        // Personal expenses — only where current user is the payer
        List<Expense> myExpenses = realExpenses.stream()
            .filter(e -> e.getPayer() != null && e.getPayer().getId().equals(user.getId()))
            .collect(Collectors.toList());

        // Spending by category — current user's own payments only
        Map<String, BigDecimal> byCategory = myExpenses.stream()
            .collect(Collectors.groupingBy(
                e -> e.getCategory() != null ? e.getCategory() : "General",
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
            ));

        // Monthly totals — current user's payments last 6 months
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        Map<String, BigDecimal> monthly = myExpenses.stream()
            .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(sixMonthsAgo))
            .collect(Collectors.groupingBy(
                e -> e.getCreatedAt().getYear() + "-" +
                     String.format("%02d", e.getCreatedAt().getMonthValue()),
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
            ));

        // Spending by member — all real expenses (excluding settlements) to show household contribution
        Map<String, BigDecimal> byMember = realExpenses.stream()
            .filter(e -> e.getPayer() != null)
            .collect(Collectors.groupingBy(
                e -> e.getPayer().getName(),
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
            ));

        // Largest expense — current user's own expenses only
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
