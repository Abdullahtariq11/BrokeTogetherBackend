package com.broketogether.api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.broketogether.api.model.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

  List<Expense> findByHomeId(Long homeId);

  Page<Expense> findByHomeIdOrderByIdDesc(Long homeId, Pageable pageable);

  List<Expense> findByPayerId(Long payerId);

  /** Flexible search used by B3 — all filters are optional (pass null to skip). */
  @Query("""
      SELECT e FROM Expense e
      WHERE e.home.id = :homeId
        AND (:query     IS NULL OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:category  IS NULL OR LOWER(e.category)    = LOWER(:category))
        AND (:payerId   IS NULL OR e.payer.id            = :payerId)
        AND (:from      IS NULL OR e.createdAt          >= :from)
        AND (:to        IS NULL OR e.createdAt          <= :to)
        AND (:type      IS NULL
             OR (:type = 'settlement' AND LOWER(e.category) = 'settlement')
             OR (:type = 'expense'    AND LOWER(e.category) <> 'settlement'))
      """)
  Page<Expense> search(
      @Param("homeId")   Long homeId,
      @Param("query")    String query,
      @Param("category") String category,
      @Param("payerId")  Long payerId,
      @Param("from")     LocalDateTime from,
      @Param("to")       LocalDateTime to,
      @Param("type")     String type,
      Pageable pageable
  );

  void deleteByHomeId(Long homeId);

  /** All SETTLEMENT-category expenses for a home, newest first. */
  @Query("SELECT e FROM Expense e WHERE e.home.id = :homeId AND LOWER(e.category) = 'settlement' ORDER BY e.createdAt DESC")
  List<Expense> findSettlementsByHomeId(@Param("homeId") Long homeId);
}
