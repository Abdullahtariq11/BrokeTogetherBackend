package com.broketogether.api.scheduler;

import com.broketogether.api.model.*;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.RecurringExpenseRepository;
import com.broketogether.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecurringExpenseSchedulerTest {

    @Mock private RecurringExpenseRepository recurringExpenseRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private HomeRepository homeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RecurringExpenseScheduler scheduler;

    private User payer;
    private Home home;

    @BeforeEach
    void setUp() {
        payer = new User("Payer", "payer@example.com", "password");
        payer.setId(1L);

        home = new Home();
        home.setId(1L);
        home.setName("Test Home");
    }

    @Test
    @DisplayName("Should create expense and advance monthly nextDueDate")
    void shouldCreateExpenseAndAdvanceMonthlyDate() {
        RecurringExpense recurring = buildRecurring(1L, RecurringFrequency.MONTHLY,
            LocalDate.now().minusDays(1));

        when(recurringExpenseRepository.findByActiveTrueAndNextDueDateLessThanEqual(any()))
            .thenReturn(List.of(recurring));
        when(homeRepository.findById(1L)).thenReturn(Optional.of(home));
        when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
        when(expenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(recurringExpenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processDueRecurringExpenses();

        // Expense should be created
        verify(expenseRepository, times(1)).save(any(Expense.class));

        // nextDueDate should advance by 1 month
        ArgumentCaptor<RecurringExpense> captor = ArgumentCaptor.forClass(RecurringExpense.class);
        verify(recurringExpenseRepository, times(1)).save(captor.capture());
        assertEquals(LocalDate.now().minusDays(1).plusMonths(1), captor.getValue().getNextDueDate());
    }

    @Test
    @DisplayName("Should create expense and advance weekly nextDueDate")
    void shouldCreateExpenseAndAdvanceWeeklyDate() {
        RecurringExpense recurring = buildRecurring(2L, RecurringFrequency.WEEKLY,
            LocalDate.now().minusDays(1));

        when(recurringExpenseRepository.findByActiveTrueAndNextDueDateLessThanEqual(any()))
            .thenReturn(List.of(recurring));
        when(homeRepository.findById(1L)).thenReturn(Optional.of(home));
        when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
        when(expenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(recurringExpenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processDueRecurringExpenses();

        ArgumentCaptor<RecurringExpense> captor = ArgumentCaptor.forClass(RecurringExpense.class);
        verify(recurringExpenseRepository, times(1)).save(captor.capture());
        assertEquals(LocalDate.now().minusDays(1).plusWeeks(1), captor.getValue().getNextDueDate());
    }

    @Test
    @DisplayName("Should skip recurring expense when home not found")
    void shouldSkipWhenHomeNotFound() {
        RecurringExpense recurring = buildRecurring(1L, RecurringFrequency.MONTHLY,
            LocalDate.now().minusDays(1));

        when(recurringExpenseRepository.findByActiveTrueAndNextDueDateLessThanEqual(any()))
            .thenReturn(List.of(recurring));
        when(homeRepository.findById(1L)).thenReturn(Optional.empty());

        scheduler.processDueRecurringExpenses();

        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process nothing when no due recurring expenses")
    void shouldProcessNothingWhenNoneDue() {
        when(recurringExpenseRepository.findByActiveTrueAndNextDueDateLessThanEqual(any()))
            .thenReturn(List.of());

        scheduler.processDueRecurringExpenses();

        verify(expenseRepository, never()).save(any());
        verify(recurringExpenseRepository, never()).save(any());
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private RecurringExpense buildRecurring(Long id, RecurringFrequency frequency,
                                             LocalDate nextDueDate) {
        RecurringExpense r = new RecurringExpense();
        r.setId(id);
        r.setHomeId(1L);
        r.setDescription("Rent");
        r.setAmount(new BigDecimal("800.00"));
        r.setCategory("Rent");
        r.setFrequency(frequency);
        r.setPayerId(1L);
        r.setNextDueDate(nextDueDate);
        r.setActive(true);
        return r;
    }
}
