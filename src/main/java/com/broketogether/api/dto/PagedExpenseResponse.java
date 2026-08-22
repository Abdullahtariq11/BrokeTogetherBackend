package com.broketogether.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PagedExpenseResponse {

  private List<ExpenseResponse> expenses;
  private boolean hasMore;
  private int page;
  private LocalDateTime lastSettledAt;

  public PagedExpenseResponse(List<ExpenseResponse> expenses, boolean hasMore, int page,
      LocalDateTime lastSettledAt) {
    this.expenses = expenses;
    this.hasMore = hasMore;
    this.page = page;
    this.lastSettledAt = lastSettledAt;
  }

  public List<ExpenseResponse> getExpenses() { return expenses; }

  public boolean isHasMore() { return hasMore; }

  public int getPage() { return page; }

  public LocalDateTime getLastSettledAt() { return lastSettledAt; }
}
