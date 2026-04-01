package com.siddhesh.QuickCart.Repository;

import com.siddhesh.QuickCart.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
