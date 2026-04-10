package com.siddhesh.QuickCart.Repository;

import com.siddhesh.QuickCart.Entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}