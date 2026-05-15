package com.broketogether.api.repository;

import com.broketogether.api.model.ShoppingItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem,Long> {
}
