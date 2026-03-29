package com.broketogether.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.broketogether.api.dto.ExpenseRequest;
import com.broketogether.api.dto.ExpenseResponse;
import com.broketogether.api.dto.ExpenseSplitResponse;
import com.broketogether.api.exception.GlobalExceptionHandler;
import com.broketogether.api.service.ExpenseService;

@ExtendWith(MockitoExtension.class)
public class ExpenseControllerTest {

  private MockMvc mockMvc;

  @Mock
  private ExpenseService expenseService;

  @InjectMocks
  private ExpenseController expenseController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(expenseController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  private ExpenseResponse createMockResponse(Long id, BigDecimal amount, String category) {
    Map<Long, ExpenseSplitResponse> splits = Map.of(
        1L, new ExpenseSplitResponse(10L, amount.divide(BigDecimal.valueOf(2))),
        2L, new ExpenseSplitResponse(11L, amount.divide(BigDecimal.valueOf(2))));
    return new ExpenseResponse(id, amount, "Test expense", category, splits);
  }

  // ==================== Create Expense (Equal Split) Tests ====================

  @Nested
  @DisplayName("POST /api/v1/expenses")
  class CreateExpenseTests {

    @Test
    @DisplayName("Should create expense successfully")
    void shouldCreateExpenseSuccessfully() throws Exception {
      ExpenseResponse response = createMockResponse(1L, new BigDecimal("100.00"), "GROCERIES");
      when(expenseService.createExpense(any(ExpenseRequest.class))).thenReturn(response);

      mockMvc.perform(post("/api/v1/expenses")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"amount\":100.00,\"category\":\"GROCERIES\",\"description\":\"Weekly groceries\"}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    @DisplayName("Should return 400 when homeId is missing")
    void shouldReturn400WhenHomeIdMissing() throws Exception {
      mockMvc.perform(post("/api/v1/expenses")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"amount\":100.00,\"category\":\"GROCERIES\",\"description\":\"Test\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when amount is missing")
    void shouldReturn400WhenAmountMissing() throws Exception {
      mockMvc.perform(post("/api/v1/expenses")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"category\":\"GROCERIES\",\"description\":\"Test\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when amount is zero")
    void shouldReturn400WhenAmountZero() throws Exception {
      mockMvc.perform(post("/api/v1/expenses")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"amount\":0,\"category\":\"GROCERIES\",\"description\":\"Test\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when category is blank")
    void shouldReturn400WhenCategoryBlank() throws Exception {
      mockMvc.perform(post("/api/v1/expenses")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"amount\":100.00,\"category\":\"\",\"description\":\"Test\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return error when home not found")
    void shouldReturnErrorWhenHomeNotFound() throws Exception {
      when(expenseService.createExpense(any(ExpenseRequest.class)))
          .thenThrow(new RuntimeException("Home with this id doesnot exist."));

      mockMvc.perform(post("/api/v1/expenses")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":999,\"amount\":100.00,\"category\":\"GROCERIES\",\"description\":\"Test\"}"))
          .andExpect(status().isNotFound());
    }
  }

  // ==================== Create Selective Expense Tests ====================

  @Nested
  @DisplayName("POST /api/v1/expenses/selective")
  class CreateSelectiveExpenseTests {

    @Test
    @DisplayName("Should create selective expense successfully")
    void shouldCreateSelectiveExpenseSuccessfully() throws Exception {
      ExpenseResponse response = createMockResponse(1L, new BigDecimal("90.00"), "DINNER");
      when(expenseService.createExpense(any(com.broketogether.api.dto.ExpenseWithUserRequest.class)))
          .thenReturn(response);

      mockMvc.perform(post("/api/v1/expenses/selective")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"amount\":90.00,\"category\":\"DINNER\",\"description\":\"Dinner out\",\"userId\":[2]}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.amount").value(90.00));
    }

    @Test
    @DisplayName("Should return 400 when userId set is empty")
    void shouldReturn400WhenUserIdEmpty() throws Exception {
      mockMvc.perform(post("/api/v1/expenses/selective")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"amount\":90.00,\"category\":\"DINNER\",\"description\":\"Test\",\"userId\":[]}"))
          .andExpect(status().isBadRequest());
    }
  }

  // ==================== Get Balances Tests ====================

  @Nested
  @DisplayName("GET /api/v1/expenses/home/{homeId}/balances")
  class GetBalancesTests {

    @Test
    @DisplayName("Should return balances")
    void shouldReturnBalances() throws Exception {
      Map<Long, BigDecimal> balances = Map.of(
          1L, new BigDecimal("50.00"),
          2L, new BigDecimal("-50.00"));
      when(expenseService.getHomeBalances(1L)).thenReturn(balances);

      mockMvc.perform(get("/api/v1/expenses/home/1/balances"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.1").value(50.00))
          .andExpect(jsonPath("$.2").value(-50.00));
    }

    @Test
    @DisplayName("Should return 403 when user is not a member")
    void shouldReturn403WhenNotMember() throws Exception {
      when(expenseService.getHomeBalances(1L))
          .thenThrow(new RuntimeException("You are not a member of this home"));

      mockMvc.perform(get("/api/v1/expenses/home/1/balances"))
          .andExpect(status().isForbidden());
    }
  }

  // ==================== Get Expense History Tests ====================

  @Nested
  @DisplayName("GET /api/v1/expenses/home/{homeId}/history")
  class GetExpenseHistoryTests {

    @Test
    @DisplayName("Should return expense history")
    void shouldReturnExpenseHistory() throws Exception {
      List<ExpenseResponse> responses = List.of(
          createMockResponse(1L, new BigDecimal("100.00"), "GROCERIES"),
          createMockResponse(2L, new BigDecimal("50.00"), "UTILITIES"));
      when(expenseService.getAllExpensesForHome(1L)).thenReturn(responses);

      mockMvc.perform(get("/api/v1/expenses/home/1/history"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should return empty list when no expenses")
    void shouldReturnEmptyListWhenNoExpenses() throws Exception {
      when(expenseService.getAllExpensesForHome(1L)).thenReturn(Collections.emptyList());

      mockMvc.perform(get("/api/v1/expenses/home/1/history"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  // ==================== Get Expense By ID Tests ====================

  @Nested
  @DisplayName("GET /api/v1/expenses/expense/{expenseId}")
  class GetExpenseByIdTests {

    @Test
    @DisplayName("Should return expense by ID")
    void shouldReturnExpenseById() throws Exception {
      ExpenseResponse response = createMockResponse(1L, new BigDecimal("100.00"), "GROCERIES");
      when(expenseService.getExpenseById(1L)).thenReturn(response);

      mockMvc.perform(get("/api/v1/expenses/expense/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Should return error when expense not found")
    void shouldReturnErrorWhenExpenseNotFound() throws Exception {
      when(expenseService.getExpenseById(999L))
          .thenThrow(new RuntimeException("Expense not found"));

      mockMvc.perform(get("/api/v1/expenses/expense/999"))
          .andExpect(status().isNotFound());
    }
  }

  // ==================== Delete Expense Tests ====================

  @Nested
  @DisplayName("DELETE /api/v1/expenses/{expenseId}")
  class DeleteExpenseTests {

    @Test
    @DisplayName("Should delete expense successfully")
    void shouldDeleteExpenseSuccessfully() throws Exception {
      doNothing().when(expenseService).deleteExpense(1L);

      mockMvc.perform(delete("/api/v1/expenses/1"))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return error when not payer")
    void shouldReturnErrorWhenNotPayer() throws Exception {
      doThrow(new RuntimeException("Only the payer can delete this expense"))
          .when(expenseService).deleteExpense(1L);

      mockMvc.perform(delete("/api/v1/expenses/1"))
          .andExpect(status().isInternalServerError());
    }
  }

  // ==================== Settle Up Tests ====================

  @Nested
  @DisplayName("POST /api/v1/expenses/settle")
  class SettleUpTests {

    @Test
    @DisplayName("Should settle up successfully")
    void shouldSettleUpSuccessfully() throws Exception {
      Map<Long, ExpenseSplitResponse> splits = Map.of(
          2L, new ExpenseSplitResponse(1L, new BigDecimal("50.00")));
      ExpenseResponse response = new ExpenseResponse(1L, new BigDecimal("50.00"),
          "Payment from Test to Other", "SETTLEMENT", splits);
      when(expenseService.settleUp(eq(1L), eq(2L), any(BigDecimal.class))).thenReturn(response);

      mockMvc.perform(post("/api/v1/expenses/settle")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"payeeId\":2,\"amount\":50.00}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.amount").value(50.00));
    }

    @Test
    @DisplayName("Should return 400 when homeId is missing")
    void shouldReturn400WhenHomeIdMissing() throws Exception {
      mockMvc.perform(post("/api/v1/expenses/settle")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"payeeId\":2,\"amount\":50.00}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when payeeId is missing")
    void shouldReturn400WhenPayeeIdMissing() throws Exception {
      mockMvc.perform(post("/api/v1/expenses/settle")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"amount\":50.00}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when amount is zero")
    void shouldReturn400WhenAmountZero() throws Exception {
      mockMvc.perform(post("/api/v1/expenses/settle")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"payeeId\":2,\"amount\":0}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return error when users not in same home")
    void shouldReturnErrorWhenUsersNotInSameHome() throws Exception {
      when(expenseService.settleUp(eq(1L), eq(99L), any(BigDecimal.class)))
          .thenThrow(new RuntimeException("Both users must be members of the same home to settle up"));

      mockMvc.perform(post("/api/v1/expenses/settle")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"homeId\":1,\"payeeId\":99,\"amount\":50.00}"))
          .andExpect(status().isForbidden());
    }
  }
}
