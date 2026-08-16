package com.broketogether.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;

import javax.security.auth.login.AccountNotFoundException;

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

import com.broketogether.api.dto.ExpenseRequest;
import com.broketogether.api.dto.ExpenseRequestUpdated;
import com.broketogether.api.dto.ExpenseResponse;
import com.broketogether.api.dto.ExpenseWithUserRequest;
import com.broketogether.api.dto.enums.SplitType;
import com.broketogether.api.model.Expense;
import com.broketogether.api.model.ExpenseSplit;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.ExpenseSplitRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

  @Mock
  private ExpenseRepository expenseRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HomeRepository homeRepository;

  @Mock
  private ExpenseSplitRepository expenseSplitRepository;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private ExpenseService expenseService;

  private User testUser;
  private User otherUser;
  private Home testHome;

  @BeforeEach
  void setUp() {
    testUser = new User("Test User", "test@example.com", "password");
    testUser.setId(1L);

    otherUser = new User("Other User", "other@example.com", "password");
    otherUser.setId(2L);

    testHome = new Home();
    testHome.setId(1L);
    testHome.setName("Test Home");
    testHome.setMembers(new HashSet<>(Set.of(testUser, otherUser)));

    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(testUser);
    SecurityContextHolder.setContext(securityContext);
  }

  // ==================== createExpense (Equal Split) Tests ====================

  @Nested
  @DisplayName("createExpense with equal splits")
  class CreateExpenseEqualSplitTests {

    @Test
    @DisplayName("Should create expense with equal splits among all members")
    void shouldCreateExpenseWithEqualSplits() throws AccountNotFoundException {
      ExpenseRequest request = new ExpenseRequest();
      request.setHomeId(1L);
      request.setAmount(new BigDecimal("100.00"));
      request.setCategory("GROCERIES");
      request.setDescription("Weekly groceries");

      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
        Expense expense = invocation.getArgument(0);
        expense.setId(1L);
        long splitId = 1L;
        for (ExpenseSplit split : expense.getSplits()) {
          split.setId(splitId++);
        }
        return expense;
      });

      ExpenseResponse response = expenseService.createExpense(request);

      assertNotNull(response);
      assertEquals(new BigDecimal("100.00"), response.getAmount());
      assertEquals("GROCERIES", response.getCategory());
      assertEquals(2, response.getSplits().size());

      response.getSplits().values().forEach(split ->
          assertEquals(new BigDecimal("50.00"), split.getAmount())
      );
      verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("Should throw exception when home not found")
    void shouldThrowExceptionWhenHomeNotFound() {
      ExpenseRequest request = new ExpenseRequest();
      request.setHomeId(999L);

      when(homeRepository.findById(999L)).thenReturn(Optional.empty());

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> expenseService.createExpense(request));
      assertEquals("Home with this id does not exist.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when user is not a member of the home")
    void shouldThrowExceptionWhenUserNotMember() {
      ExpenseRequest request = new ExpenseRequest();
      request.setHomeId(1L);

      Home homeWithoutUser = new Home();
      homeWithoutUser.setId(1L);
      homeWithoutUser.setMembers(new HashSet<>(Set.of(otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(homeWithoutUser));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> expenseService.createExpense(request));
      assertEquals("You are not a member of this home", exception.getMessage());
    }
  }

  // ==================== createExpense (Custom Split) Tests ====================

  @Nested
  @DisplayName("createExpense with user-defined splits")
  class CreateExpenseCustomSplitTests {

    @Test
    @DisplayName("Should create expense split among selected users")
    void shouldCreateExpenseWithSelectedUsers() throws AccountNotFoundException {
      User thirdUser = new User("Third User", "third@example.com", "password");
      thirdUser.setId(3L);
      testHome.getMembers().add(thirdUser);

      ExpenseWithUserRequest request = new ExpenseWithUserRequest();
      request.setHomeId(1L);
      request.setAmount(new BigDecimal("90.00"));
      request.setCategory("DINNER");
      request.setDescription("Dinner out");
      request.setUserId(Set.of(2L));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(otherUser));
      when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
        Expense expense = invocation.getArgument(0);
        expense.setId(1L);
        long splitId = 1L;
        for (ExpenseSplit split : expense.getSplits()) {
          split.setId(splitId++);
        }
        return expense;
      });

      ExpenseResponse response = expenseService.createExpense(request);

      assertNotNull(response);
      assertEquals(2, response.getSplits().size());
      response.getSplits().values().forEach(split ->
          assertEquals(new BigDecimal("45.00"), split.getAmount())
      );
    }

    @Test
    @DisplayName("Should throw exception when home has only one member")
    void shouldThrowExceptionWhenNotEnoughMembers() {
      ExpenseWithUserRequest request = new ExpenseWithUserRequest();
      request.setHomeId(1L);

      Home singleMemberHome = new Home();
      singleMemberHome.setId(1L);
      singleMemberHome.setMembers(new HashSet<>(Set.of(testUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(singleMemberHome));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> expenseService.createExpense(request));
      assertEquals("Not enough members in the home to split.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when selected user not in home")
    void shouldThrowExceptionWhenUserNotInHome() {
      User outsider = new User("Outsider", "outsider@example.com", "password");
      outsider.setId(99L);

      ExpenseWithUserRequest request = new ExpenseWithUserRequest();
      request.setHomeId(1L);
      request.setAmount(new BigDecimal("100.00"));
      request.setUserId(Set.of(99L));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(userRepository.findAllById(Set.of(99L))).thenReturn(List.of(outsider));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> expenseService.createExpense(request));
      assertTrue(exception.getMessage().contains("is not a member of this home"));
    }
  }

  // ==================== createExpenseUpdated (SplitType) Tests ====================

  @Nested
  @DisplayName("createExpenseUpdated with split types")
  class CreateExpenseUpdatedTests {

    private ExpenseRequestUpdated buildRequest(SplitType splitType, Set<Long> userIds,
        Map<Long, BigDecimal> exactSplits, BigDecimal payerFixed) {
      ExpenseRequestUpdated req = new ExpenseRequestUpdated(
          new BigDecimal("120.00"), "Test expense", "FOOD",
          userIds, exactSplits, splitType, payerFixed);
      req.setHomeId(1L);
      return req;
    }

    private void stubSave() {
      when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
        Expense e = inv.getArgument(0);
        e.setId(10L);
        long sid = 100L;
        for (ExpenseSplit s : e.getSplits()) s.setId(sid++);
        return e;
      });
    }

    @Test
    @DisplayName("EQUAL split with explicit userIds divides evenly")
    void equalSplitWithUserIds() throws AccountNotFoundException {
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(otherUser));
      stubSave();

      ExpenseResponse response = expenseService.createExpenseUpdated(
          buildRequest(SplitType.EQUAL, Set.of(2L), null, null));

      assertEquals(2, response.getSplits().size());
      response.getSplits().values().forEach(s ->
          assertEquals(new BigDecimal("60.00"), s.getAmount()));
    }

    @Test
    @DisplayName("EQUAL split with no userIds defaults to all home members")
    void equalSplitDefaultsToAllMembers() throws AccountNotFoundException {
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      stubSave();

      ExpenseResponse response = expenseService.createExpenseUpdated(
          buildRequest(SplitType.EQUAL, null, null, null));

      assertEquals(2, response.getSplits().size());
      response.getSplits().values().forEach(s ->
          assertEquals(new BigDecimal("60.00"), s.getAmount()));
    }

    @Test
    @DisplayName("PERSONAL split records full amount only for payer")
    void personalSplitOnlyForPayer() throws AccountNotFoundException {
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      stubSave();

      ExpenseResponse response = expenseService.createExpenseUpdated(
          buildRequest(SplitType.PERSONAL, null, null, null));

      assertEquals(1, response.getSplits().size());
      BigDecimal splitAmount = response.getSplits().get(testUser.getId()).getAmount();
      assertEquals(new BigDecimal("120.00"), splitAmount);
    }

    @Test
    @DisplayName("FIXED split assigns payer's fixed amount and remainder to others")
    void fixedSplitAssignsCorrectAmounts() throws AccountNotFoundException {
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(otherUser));
      stubSave();

      // Payer pays 80, other pays 40
      ExpenseResponse response = expenseService.createExpenseUpdated(
          buildRequest(SplitType.FIXED, Set.of(2L), null, new BigDecimal("80.00")));

      assertEquals(2, response.getSplits().size());
      assertEquals(new BigDecimal("80.00"), response.getSplits().get(testUser.getId()).getAmount());
      assertEquals(new BigDecimal("40.00"), response.getSplits().get(otherUser.getId()).getAmount());
    }

    @Test
    @DisplayName("CUSTOM split uses exact per-user amounts")
    void customSplitUsesExactAmounts() throws AccountNotFoundException {
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(otherUser));
      stubSave();

      Map<Long, BigDecimal> exact = Map.of(
          1L, new BigDecimal("70.00"),
          2L, new BigDecimal("50.00"));

      ExpenseResponse response = expenseService.createExpenseUpdated(
          buildRequest(SplitType.CUSTOM, Set.of(2L), exact, null));

      assertEquals(2, response.getSplits().size());
      assertEquals(new BigDecimal("70.00"), response.getSplits().get(testUser.getId()).getAmount());
      assertEquals(new BigDecimal("50.00"), response.getSplits().get(otherUser.getId()).getAmount());
    }

    @Test
    @DisplayName("CUSTOM split throws when amounts do not sum to total")
    void customSplitThrowsOnMismatch() {
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(otherUser));

      Map<Long, BigDecimal> badExact = Map.of(
          1L, new BigDecimal("60.00"),
          2L, new BigDecimal("50.00")); // sums to 110, not 120

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> expenseService.createExpenseUpdated(
              buildRequest(SplitType.CUSTOM, Set.of(2L), badExact, null)));
      assertTrue(ex.getMessage().contains("must equal total"));
    }

    @Test
    @DisplayName("FIXED split throws when payerFixedAmount exceeds total")
    void fixedSplitThrowsWhenPayerAmountExceedsTotal() {
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(otherUser));

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> expenseService.createExpenseUpdated(
              buildRequest(SplitType.FIXED, Set.of(2L), null, new BigDecimal("150.00"))));
      assertTrue(ex.getMessage().contains("Invalid payer fixed amount"));
    }

    @Test
    @DisplayName("Throws when selected user is not a home member")
    void throwsWhenSelectedUserNotInHome() {
      User outsider = new User("Out", "out@test.com", "pw");
      outsider.setId(99L);

      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(userRepository.findAllById(Set.of(99L))).thenReturn(List.of(outsider));

      assertThrows(RuntimeException.class,
          () -> expenseService.createExpenseUpdated(
              buildRequest(SplitType.EQUAL, Set.of(99L), null, null)));
    }

    @Test
    @DisplayName("Throws when home not found")
    void throwsWhenHomeNotFound() {
      when(homeRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(RuntimeException.class,
          () -> expenseService.createExpenseUpdated(
              buildRequest(SplitType.EQUAL, null, null, null)));
    }
  }

  // ==================== getAllExpensesForHome Tests ====================

  @Nested
  @DisplayName("getAllExpensesForHome")
  class GetAllExpensesTests {

    @Test
    @DisplayName("Should return all expenses for a home")
    void shouldReturnAllExpenses() throws AccountNotFoundException {
      Expense expense1 = createMockExpense(1L, new BigDecimal("100.00"), "GROCERIES");
      Expense expense2 = createMockExpense(2L, new BigDecimal("50.00"), "UTILITIES");

      org.springframework.data.domain.Page<Expense> page =
          new org.springframework.data.domain.PageImpl<>(List.of(expense1, expense2));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(expenseRepository.findByHomeIdOrderByIdDesc(eq(1L), any())).thenReturn(page);

      com.broketogether.api.dto.PagedExpenseResponse result =
          expenseService.getAllExpensesForHome(1L, 0, 20);

      assertEquals(2, result.getExpenses().size());
      assertFalse(result.isHasMore());
    }

    @Test
    @DisplayName("Should return empty list when no expenses exist")
    void shouldReturnEmptyListWhenNoExpenses() throws AccountNotFoundException {
      org.springframework.data.domain.Page<Expense> emptyPage =
          new org.springframework.data.domain.PageImpl<>(Collections.emptyList());

      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(expenseRepository.findByHomeIdOrderByIdDesc(eq(1L), any())).thenReturn(emptyPage);

      com.broketogether.api.dto.PagedExpenseResponse result =
          expenseService.getAllExpensesForHome(1L, 0, 20);

      assertTrue(result.getExpenses().isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when user not a member")
    void shouldThrowWhenNotMember() {
      Home homeWithoutUser = new Home();
      homeWithoutUser.setId(1L);
      homeWithoutUser.setMembers(new HashSet<>(Set.of(otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(homeWithoutUser));

      assertThrows(RuntimeException.class,
          () -> expenseService.getAllExpensesForHome(1L, 0, 20));
    }
  }

  // ==================== getExpenseById Tests ====================

  @Nested
  @DisplayName("getExpenseById")
  class GetExpenseByIdTests {

    @Test
    @DisplayName("Should return expense by ID")
    void shouldReturnExpenseById() throws AccountNotFoundException {
      Expense expense = createMockExpense(1L, new BigDecimal("100.00"), "GROCERIES");
      expense.setHome(testHome);

      when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

      ExpenseResponse response = expenseService.getExpenseById(1L);

      assertNotNull(response);
      assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Should throw exception when expense not found")
    void shouldThrowExceptionWhenExpenseNotFound() {
      when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

      assertThrows(RuntimeException.class,
          () -> expenseService.getExpenseById(999L));
    }
  }

  // ==================== getHomeBalances Tests ====================

  @Nested
  @DisplayName("getHomeBalances")
  class GetHomeBalancesTests {

    @Test
    @DisplayName("Should calculate correct balances")
    void shouldCalculateCorrectBalances() throws AccountNotFoundException {
      Expense expense = createMockExpense(1L, new BigDecimal("100.00"), "GROCERIES");
      expense.setPayer(testUser);

      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(expenseRepository.findByHomeId(1L)).thenReturn(List.of(expense));

      Map<Long, BigDecimal> balances = expenseService.getHomeBalances(1L);

      assertEquals(new BigDecimal("50.00"), balances.get(1L));
      assertEquals(new BigDecimal("-50.00"), balances.get(2L));
    }

    @Test
    @DisplayName("Should return zero balances when no expenses")
    void shouldReturnZeroBalancesWhenNoExpenses() throws AccountNotFoundException {
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(expenseRepository.findByHomeId(1L)).thenReturn(Collections.emptyList());

      Map<Long, BigDecimal> balances = expenseService.getHomeBalances(1L);

      assertEquals(new BigDecimal("0.00"), balances.get(1L));
      assertEquals(new BigDecimal("0.00"), balances.get(2L));
    }

    @Test
    @DisplayName("Should throw exception when user is not a member")
    void shouldThrowWhenNotMember() {
      Home homeWithoutUser = new Home();
      homeWithoutUser.setId(1L);
      homeWithoutUser.setMembers(new HashSet<>(Set.of(otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(homeWithoutUser));

      assertThrows(RuntimeException.class,
          () -> expenseService.getHomeBalances(1L));
    }
  }

  // ==================== deleteExpense Tests ====================

  @Nested
  @DisplayName("deleteExpense")
  class DeleteExpenseTests {

    @Test
    @DisplayName("Should delete expense when user is payer")
    void shouldDeleteExpenseWhenUserIsPayer() throws AccountNotFoundException {
      Expense expense = createMockExpense(1L, new BigDecimal("100.00"), "GROCERIES");
      expense.setPayer(testUser);

      when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

      assertDoesNotThrow(() -> expenseService.deleteExpense(1L));
      verify(expenseRepository, times(1)).delete(expense);
    }

    @Test
    @DisplayName("Should throw exception when user is not payer")
    void shouldThrowExceptionWhenUserNotPayer() {
      Expense expense = createMockExpense(1L, new BigDecimal("100.00"), "GROCERIES");
      expense.setPayer(otherUser);

      when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> expenseService.deleteExpense(1L));
      assertEquals("Only the payer can delete this expense", exception.getMessage());
      verify(expenseRepository, never()).delete(any());
    }
  }

  // ==================== settleUp Tests ====================

  @Nested
  @DisplayName("settleUp")
  class SettleUpTests {

    @Test
    @DisplayName("Should create settlement expense")
    void shouldCreateSettlement() throws AccountNotFoundException {
      when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
      when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
        Expense expense = invocation.getArgument(0);
        expense.setId(1L);
        expense.getSplits().get(0).setId(1L);
        return expense;
      });

      ExpenseResponse response = expenseService.settleUp(1L, 2L, new BigDecimal("50.00"));

      assertNotNull(response);
      assertEquals("SETTLEMENT", response.getCategory());
      assertEquals(new BigDecimal("50.00"), response.getAmount());
      assertTrue(response.getDescription().contains("Payment from"));
    }

    @Test
    @DisplayName("Should throw exception when payee not found")
    void shouldThrowExceptionWhenPayeeNotFound() {
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      assertThrows(RuntimeException.class,
          () -> expenseService.settleUp(1L, 999L, new BigDecimal("50.00")));
    }

    @Test
    @DisplayName("Should throw exception when users not in same home")
    void shouldThrowExceptionWhenUsersNotInSameHome() {
      User outsider = new User("Outsider", "outsider@example.com", "password");
      outsider.setId(99L);

      when(userRepository.findById(99L)).thenReturn(Optional.of(outsider));
      when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> expenseService.settleUp(1L, 99L, new BigDecimal("50.00")));
      assertEquals("Both users must be members of the same home to settle up", exception.getMessage());
    }
  }

  // ==================== Helper Methods ====================

  private Expense createMockExpense(Long id, BigDecimal amount, String category) {
    Expense expense = new Expense();
    expense.setId(id);
    expense.setAmount(amount);
    expense.setCategory(category);
    expense.setDescription("Test expense");
    expense.setPayer(testUser);
    expense.setHome(testHome);

    ExpenseSplit split1 = new ExpenseSplit(expense, testUser, amount.divide(BigDecimal.valueOf(2)));
    split1.setId(id * 10);
    ExpenseSplit split2 = new ExpenseSplit(expense, otherUser, amount.divide(BigDecimal.valueOf(2)));
    split2.setId(id * 10 + 1);

    expense.setSplits(List.of(split1, split2));
    return expense;
  }
}
