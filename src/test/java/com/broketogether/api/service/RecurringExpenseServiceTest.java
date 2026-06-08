package com.broketogether.api.service;

import com.broketogether.api.dto.RecurringExpenseRequest;
import com.broketogether.api.dto.RecurringExpenseResponse;
import com.broketogether.api.model.*;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.RecurringExpenseRepository;
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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecurringExpenseServiceTest {

    @Mock private RecurringExpenseRepository recurringExpenseRepository;
    @Mock private HomeRepository homeRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private RecurringExpenseService recurringExpenseService;

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

    // ==================== create Tests ====================

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Should create recurring expense for premium user")
        void shouldCreateRecurringExpense() throws Exception {
            when(authentication.getPrincipal()).thenReturn(premiumUser);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

            RecurringExpense saved = buildRecurring(1L, "Rent", new BigDecimal("800.00"),
                "Rent", RecurringFrequency.MONTHLY, premiumUser.getId());
            when(recurringExpenseRepository.save(any())).thenReturn(saved);

            RecurringExpenseRequest request = buildRequest(1L, "Rent", new BigDecimal("800.00"),
                "Rent", RecurringFrequency.MONTHLY);

            RecurringExpenseResponse response = recurringExpenseService.create(request);

            assertNotNull(response);
            assertEquals("Rent", response.description());
            assertEquals(new BigDecimal("800.00"), response.amount());
            assertEquals(RecurringFrequency.MONTHLY, response.frequency());
            verify(recurringExpenseRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Should throw 403 for free user")
        void shouldThrow403ForFreeUser() {
            when(authentication.getPrincipal()).thenReturn(freeUser);

            RecurringExpenseRequest request = buildRequest(1L, "Rent",
                new BigDecimal("800.00"), "Rent", RecurringFrequency.MONTHLY);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recurringExpenseService.create(request));

            assertEquals(403, ex.getStatusCode().value());
            verify(recurringExpenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw 403 when user is not a member")
        void shouldThrow403WhenNotMember() {
            User outsider = new User("Outsider", "out@example.com", "password");
            outsider.setId(99L);
            outsider.setPremium(true);

            when(authentication.getPrincipal()).thenReturn(outsider);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

            RecurringExpenseRequest request = buildRequest(1L, "Rent",
                new BigDecimal("800.00"), "Rent", RecurringFrequency.MONTHLY);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recurringExpenseService.create(request));

            assertEquals(403, ex.getStatusCode().value());
        }

        @Test
        @DisplayName("Should set nextDueDate to today on creation")
        void shouldSetNextDueDateToToday() throws Exception {
            when(authentication.getPrincipal()).thenReturn(premiumUser);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

            RecurringExpense saved = buildRecurring(1L, "Rent", new BigDecimal("800.00"),
                "Rent", RecurringFrequency.MONTHLY, premiumUser.getId());
            saved.setNextDueDate(LocalDate.now());
            when(recurringExpenseRepository.save(any())).thenReturn(saved);

            RecurringExpenseRequest request = buildRequest(1L, "Rent",
                new BigDecimal("800.00"), "Rent", RecurringFrequency.MONTHLY);

            RecurringExpenseResponse response = recurringExpenseService.create(request);

            assertEquals(LocalDate.now(), response.nextDueDate());
        }
    }

    // ==================== getByHome Tests ====================

    @Nested
    @DisplayName("getByHome")
    class GetByHomeTests {

        @Test
        @DisplayName("Should return active recurring expenses for home")
        void shouldReturnActiveRecurringExpenses() throws Exception {
            when(authentication.getPrincipal()).thenReturn(premiumUser);
            when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

            RecurringExpense r1 = buildRecurring(1L, "Rent", new BigDecimal("800.00"),
                "Rent", RecurringFrequency.MONTHLY, premiumUser.getId());
            RecurringExpense r2 = buildRecurring(2L, "Netflix", new BigDecimal("15.00"),
                "Entertainment", RecurringFrequency.MONTHLY, freeUser.getId());

            when(recurringExpenseRepository.findByHomeIdAndActiveTrue(1L))
                .thenReturn(List.of(r1, r2));

            List<RecurringExpenseResponse> result = recurringExpenseService.getByHome(1L);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Should throw 403 for free user")
        void shouldThrow403ForFreeUser() {
            when(authentication.getPrincipal()).thenReturn(freeUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recurringExpenseService.getByHome(1L));

            assertEquals(403, ex.getStatusCode().value());
        }
    }

    // ==================== deactivate Tests ====================

    @Nested
    @DisplayName("deactivate")
    class DeactivateTests {

        @Test
        @DisplayName("Should deactivate recurring expense when owner requests it")
        void shouldDeactivateWhenOwner() throws Exception {
            when(authentication.getPrincipal()).thenReturn(premiumUser);

            RecurringExpense recurring = buildRecurring(1L, "Rent", new BigDecimal("800.00"),
                "Rent", RecurringFrequency.MONTHLY, premiumUser.getId());
            when(recurringExpenseRepository.findById(1L)).thenReturn(Optional.of(recurring));

            recurringExpenseService.deactivate(1L);

            assertFalse(recurring.isActive());
            verify(recurringExpenseRepository, times(1)).save(recurring);
        }

        @Test
        @DisplayName("Should throw 403 when non-owner tries to deactivate")
        void shouldThrow403WhenNotOwner() {
            when(authentication.getPrincipal()).thenReturn(freeUser);

            RecurringExpense recurring = buildRecurring(1L, "Rent", new BigDecimal("800.00"),
                "Rent", RecurringFrequency.MONTHLY, premiumUser.getId()); // owned by premiumUser
            when(recurringExpenseRepository.findById(1L)).thenReturn(Optional.of(recurring));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recurringExpenseService.deactivate(1L));

            assertEquals(403, ex.getStatusCode().value());
            verify(recurringExpenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when recurring expense not found")
        void shouldThrowWhenNotFound() {
            when(authentication.getPrincipal()).thenReturn(premiumUser);
            when(recurringExpenseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                () -> recurringExpenseService.deactivate(999L));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RecurringExpenseRequest buildRequest(Long homeId, String description,
            BigDecimal amount, String category, RecurringFrequency frequency) {
        RecurringExpenseRequest r = new RecurringExpenseRequest();
        r.setHomeId(homeId);
        r.setDescription(description);
        r.setAmount(amount);
        r.setCategory(category);
        r.setFrequency(frequency);
        return r;
    }

    private RecurringExpense buildRecurring(Long id, String description, BigDecimal amount,
            String category, RecurringFrequency frequency, Long payerId) {
        RecurringExpense r = new RecurringExpense();
        r.setId(id);
        r.setHomeId(1L);
        r.setDescription(description);
        r.setAmount(amount);
        r.setCategory(category);
        r.setFrequency(frequency);
        r.setPayerId(payerId);
        r.setNextDueDate(LocalDate.now());
        r.setActive(true);
        return r;
    }
}
