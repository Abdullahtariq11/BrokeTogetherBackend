package com.broketogether.api.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AccountNotFoundException;

import com.broketogether.api.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import com.broketogether.api.service.AnalyticsService;
import com.broketogether.api.service.ExpenseService;
import com.broketogether.api.service.ExportService;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

  private final ExpenseService expenseService;
  private final ExportService exportService;
  private final AnalyticsService analyticsService;

  public ExpenseController(ExpenseService expenseService, ExportService exportService,
      AnalyticsService analyticsService) {
    this.expenseService = expenseService;
    this.exportService = exportService;
    this.analyticsService = analyticsService;
  }

  @PostMapping
  public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request)
      throws AccountNotFoundException {
    return ResponseEntity.status(201).body(expenseService.createExpense(request));
  }

  @PostMapping("/selective")
  public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseWithUserRequest request)
      throws AccountNotFoundException {
    return ResponseEntity.status(201).body(expenseService.createExpense(request));
  }

  @PostMapping("/split")
  public ResponseEntity<ExpenseResponse> createWithSplitType(
      @Valid @RequestBody ExpenseRequestUpdated request) throws AccountNotFoundException {
    return ResponseEntity.status(201).body(expenseService.createExpenseUpdated(request));
  }

  @GetMapping("/home/{homeId}/balances")
  public ResponseEntity<Map<Long, BigDecimal>> getBalancesByHomeId(@PathVariable Long homeId)
      throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.getHomeBalances(homeId));
  }

  @GetMapping("/home/{homeId}/history")
  public ResponseEntity<PagedExpenseResponse> getAllByHomeId(
      @PathVariable Long homeId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size)
      throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.getAllExpensesForHome(homeId, page, size));
  }

  @GetMapping("/expense/{expenseId}")
  public ResponseEntity<ExpenseResponse> getById(@PathVariable Long expenseId)
      throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.getExpenseById(expenseId));
  }

  @DeleteMapping("/{expenseId}")
  public ResponseEntity<Void> delete(@PathVariable Long expenseId) throws AccountNotFoundException {
    expenseService.deleteExpense(expenseId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/settle")
  public ResponseEntity<ExpenseResponse> settle(@Valid @RequestBody SettlementRequest request)
      throws AccountNotFoundException {
    return ResponseEntity.status(201).body(
        expenseService.settleUp(request.getHomeId(), request.getPayeeId(), request.getAmount()));
  }

  @GetMapping("/home/{homeId}/settlements")
  public ResponseEntity<List<SettlementSuggestion>> getSettlements(@PathVariable Long homeId)
          throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.getSettlements(homeId));
  }

  @GetMapping("/home/{homeId}/analytics")
  public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable Long homeId)
      throws AccountNotFoundException {
    return ResponseEntity.ok(analyticsService.getAnalytics(homeId));
  }

  // ── B1: patch expense ────────────────────────────────────────────────────────
  @PatchMapping("/{expenseId}")
  public ResponseEntity<ExpenseResponse> patch(
      @PathVariable Long expenseId,
      @Valid @RequestBody PatchExpenseRequest request) throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.patchExpense(expenseId, request));
  }

  // ── B2: record payment received, settlement history, remind ──────────────────
  @PostMapping("/settle/received")
  public ResponseEntity<ExpenseResponse> recordReceived(
      @Valid @RequestBody RecordPaymentRequest request) throws AccountNotFoundException {
    return ResponseEntity.status(201).body(expenseService.recordPaymentReceived(request));
  }

  @GetMapping("/home/{homeId}/settlement-history")
  public ResponseEntity<List<SettlementHistoryResponse>> settlementHistory(
      @PathVariable Long homeId) throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.getSettlementHistory(homeId));
  }

  @GetMapping("/home/{homeId}/owed-to-me")
  public ResponseEntity<List<SettlementSuggestion>> owedToMe(
      @PathVariable Long homeId) throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.getOwedToMe(homeId));
  }

  // ── B3: search/filter/sort ────────────────────────────────────────────────────
  @GetMapping("/home/{homeId}/search")
  public ResponseEntity<PagedExpenseResponse> search(
      @PathVariable Long homeId,
      @ModelAttribute ExpenseSearchRequest request) throws AccountNotFoundException {
    return ResponseEntity.ok(expenseService.searchExpenses(homeId, request));
  }

  @GetMapping("/home/{homeId}/export")
  public ResponseEntity<byte[]> export(
      @PathVariable Long homeId,
      @RequestParam String format) throws Exception {

    if ("pdf".equalsIgnoreCase(format)) {
      byte[] pdf = exportService.exportPdf(homeId);
      return ResponseEntity.ok()
          .header("Content-Disposition", "attachment; filename=expenses.pdf")
          .contentType(MediaType.APPLICATION_PDF)
          .body(pdf);
    } else {
      byte[] csv = exportService.exportCsv(homeId);
      return ResponseEntity.ok()
          .header("Content-Disposition", "attachment; filename=expenses.csv")
          .contentType(MediaType.parseMediaType("text/csv"))
          .body(csv);
    }
  }

}
