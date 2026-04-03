package com.siddhesh.QuickCart.Repository;

import com.siddhesh.QuickCart.Entity.Order;
import com.siddhesh.QuickCart.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}
