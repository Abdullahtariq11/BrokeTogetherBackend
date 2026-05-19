package com.broketogether.api.repository;

import com.broketogether.api.model.Expense;
import com.broketogether.api.model.ShoppingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem,Long> {
    List<ShoppingItem> findByHomeId(Long homeId);
    List<ShoppingItem> findByAddedById(Long userId);
    List<ShoppingItem> findByCheckedById(Long userId);
}
