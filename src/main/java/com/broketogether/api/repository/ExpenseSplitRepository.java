package com.broketogether.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.broketogether.api.model.ExpenseSplit;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

}
