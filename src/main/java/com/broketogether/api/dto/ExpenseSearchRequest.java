package com.broketogether.api.dto;

import java.time.LocalDate;

/**
 * Query parameters for expense search/filter.
 * All fields are optional — omitting a field means no filter on that dimension.
 */
public class ExpenseSearchRequest {
    private String query;         // text search on description
    private String category;      // exact category match
    private Long memberId;        // payer filter
    private LocalDate from;       // inclusive date range start
    private LocalDate to;         // inclusive date range end
    private String type;          // "expense" | "settlement" | null (both)
    private int page = 0;
    private int size = 20;
    private String sort = "date_desc"; // date_desc | date_asc | amount_desc | amount_asc

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }

    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
