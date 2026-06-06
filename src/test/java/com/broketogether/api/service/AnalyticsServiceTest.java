package com.broketogether.api.service;

import com.broketogether.api.dto.AnalyticsResponse;
import com.broketogether.api.model.Expense;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private HomeRepository homeRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User premiumUser;
    private User freeUser;
    private Home home;

    @BeforeEach
    void setUp() {
        premiumUser = new User("Premium User", "premium@example.com", "password");
        premiumUser.setId(1L);
        premiumUser.setPremium(true);

        freeUser = new User("Free User", "free@example.com", "password");
        freeUser.setId(2L);
        freeUser.setPremium(false);

        home = new Home();
        home.setId(1L);
        home.setName("Test Home");
        home.setMembers(new HashSet<>(Set.of(premiumUser, freeUser)));

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("getAnalytics")
    class GetAnalyticsTests {

        @Test
        @DisplayName("Should return analytics with correct category totals")
        void shouldReturnAnalyticsWithCategoryTotals() throws Exception {
            when(authentication.getPrincipal()).thenReturn(premiumUser);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

            Expense e1 = buildExpense(1L, "Rent", new BigDecimal("800.00"), "Rent", premiumUser);
            Expense e2 = buildExpense(2L, "Groceries run", new BigDecimal("120.00"), "Groceries", premiumUser);
            Expense e3 = buildExpense(3L, "More groceries", new BigDecimal("80.00"), "Groceries", freeUser);

            when(expenseRepository.findByHomeId(1L)).thenReturn(List.of(e1, e2, e3));

            AnalyticsResponse response = analyticsService.getAnalytics(1L);

            assertNotNull(response);
            assertEquals(new BigDecimal("800.00"), response.spendingByCategory().get("Rent"));
            assertEquals(new BigDecimal("200.00"), response.spendingByCategory().get("Groceries"));
        }

        @Test
        @DisplayName("Should return monthly totals for last 6 months")
        void shouldReturnMonthlyTotals() throws Exception {
            when(authentication.getPrincipal()).thenReturn(premiumUser);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

            Expense recent = buildExpense(1L, "Netflix", new BigDecimal("15.00"), "Entertainment", premiumUser);
            recent.setCreatedAt(LocalDateTime.now().minusMonths(1));

            Expense old = buildExpense(2L, "Old expense", new BigDecimal("500.00"), "General", premiumUser);
            old.setCreatedAt(LocalDateTime.now().minusMonths(8)); // older than 6 months

            when(expenseRepository.findByHomeId(1L)).thenReturn(List.of(recent, old));

            AnalyticsResponse response = analyticsService.getAnalytics(1L);

            // Old expense should be excluded from monthly totals
            assertEquals(1, response.monthlyTotals().size());
            assertTrue(response.monthlyTotals().values().stream()
                .anyMatch(v -> v.compareTo(new BigDecimal("15.00")) == 0));
        }

        @Test
        @DisplayName("Should return spending by member")
        void shouldReturnSpendingByMember() throws Exception {
            when(authentication.getPrincipal()).thenReturn(premiumUser);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

            Expense e1 = buildExpense(1L, "Rent", new BigDecimal("800.00"), "Rent", premiumUser);
            Expense e2 = buildExpense(2L, "Groceries", new BigDecimal("100.00"), "Groceries", freeUser);

            when(expenseRepository.findByHomeId(1L)).thenReturn(List.of(e1, e2));

            AnalyticsResponse response = analyticsService.getAnalytics(1L);

            assertEquals(new BigDecimal("800.00"), response.spendingByMember().get("Premium User"));
            assertEquals(new BigDecimal("100.00"), response.spendingByMember().get("Free User"));
        }

        @Test
        @DisplayName("Should return correct largest expense")
        void shouldReturnLargestExpense() throws Exception {
            when(authentication.getPrincipal()).thenReturn(premiumUser);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

            Expense small = buildExpense(1L, "Coffee", new BigDecimal("5.00"), "Food", premiumUser);
            Expense large = buildExpense(2L, "Annual Rent", new BigDecimal("1200.00"), "Rent", premiumUser);

            when(expenseRepository.findByHomeId(1L)).thenReturn(List.of(small, large));

            AnalyticsResponse response = analyticsService.getAnalytics(1L);

            assertEquals(new BigDecimal("1200.00"), response.largestExpenseAmount());
            assertEquals("Annual Rent", response.largestExpenseDescription());
        }

        @Test
        @DisplayName("Should return empty analytics when no expenses")
        void shouldReturnEmptyAnalyticsWhenNoExpenses() throws Exception {
            when(authentication.getPrincipal()).thenReturn(premiumUser);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));
            when(expenseRepository.findByHomeId(1L)).thenReturn(List.of());

            AnalyticsResponse response = analyticsService.getAnalytics(1L);

            assertTrue(response.spendingByCategory().isEmpty());
            assertTrue(response.monthlyTotals().isEmpty());
            assertTrue(response.spendingByMember().isEmpty());
            assertEquals(BigDecimal.ZERO, response.largestExpenseAmount());
            assertEquals("N/A", response.largestExpenseDescription());
        }

        @Test
        @DisplayName("Should throw 403 for free user")
        void shouldThrow403ForFreeUser() {
            when(authentication.getPrincipal()).thenReturn(freeUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> analyticsService.getAnalytics(1L));

            assertEquals(403, ex.getStatusCode().value());
        }

        @Test
        @DisplayName("Should throw 403 when user is not a member of the home")
        void shouldThrow403WhenNotMember() {
            User outsider = new User("Outsider", "outsider@example.com", "password");
            outsider.setId(99L);
            outsider.setPremium(true);

            when(authentication.getPrincipal()).thenReturn(outsider);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> analyticsService.getAnalytics(1L));

            assertEquals(403, ex.getStatusCode().value());
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Expense buildExpense(Long id, String description, BigDecimal amount,
                                  String category, User payer) {
        Expense e = new Expense();
        e.setId(id);
        e.setDescription(description);
        e.setAmount(amount);
        e.setCategory(category);
        e.setPayer(payer);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }
}
