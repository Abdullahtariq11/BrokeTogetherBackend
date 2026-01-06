package com.broketogether.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.broketogether.api.model.ExpenseSplit;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

}
