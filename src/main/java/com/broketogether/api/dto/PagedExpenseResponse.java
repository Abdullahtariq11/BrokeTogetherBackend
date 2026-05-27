package com.broketogether.api.dto;

import java.util.List;

public class PagedExpenseResponse {

  private List<ExpenseResponse> expenses;
  private boolean hasMore;
  private int page;

  public PagedExpenseResponse(List<ExpenseResponse> expenses, boolean hasMore, int page) {
    this.expenses = expenses;
    this.hasMore = hasMore;
    this.page = page;
  }

  public List<ExpenseResponse> getExpenses() { return expenses; }

  public boolean isHasMore() { return hasMore; }

  public int getPage() { return page; }
}
