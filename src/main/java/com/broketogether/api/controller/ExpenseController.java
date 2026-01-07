package com.broketogether.api.controller;

import java.util.List;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.broketogether.api.dto.ExpenseRequest;
import com.broketogether.api.dto.ExpenseResponse;
import com.broketogether.api.dto.ExpenseWithUserRequest;
import com.broketogether.api.service.ExpenseService;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

  private final ExpenseService expenseService;

  public ExpenseController(ExpenseService expenseService) {
    this.expenseService = expenseService;

  }

  /**
   * Path: POST /api/v1/expenses // This handles the "Split with everyone in the
   * Home" logic
   */
  @PostMapping
  public ResponseEntity<ExpenseResponse> create(@RequestBody ExpenseRequest request)
      throws AccountNotFoundException {
    return ResponseEntity.status(201).body(expenseService.createExpense(request));
  }

  /**
   * Path: POST /api/v1/expenses/selective "create" is a bit redundant in a POST
   * path, "selective" or "partial" is clearer
   */
  @PostMapping("/selective")
  public ResponseEntity<ExpenseResponse> create(@RequestBody ExpenseWithUserRequest request)
      throws AccountNotFoundException {
    return ResponseEntity.status(201).body(expenseService.createExpense(request));
  }

  @GetMapping("/{homeId}")
  public ResponseEntity<List<ExpenseResponse>> getAllByHomeId(@PathVariable Long homeId)
      throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.getAllExpensesForHome(homeId));
  }

  @GetMapping("/expense/{expenseId}")
  public ResponseEntity<ExpenseResponse> getById(@PathVariable Long expenseId)
      throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.getExpenseById(expenseId));
  }

}
