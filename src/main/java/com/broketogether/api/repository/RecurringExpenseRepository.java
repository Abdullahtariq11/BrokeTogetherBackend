package com.broketogether.api.repository;

import com.broketogether.api.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    List<RecurringExpense> findByHomeIdAndActiveTrue(Long homeId);

    List<RecurringExpense> findByActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
}
