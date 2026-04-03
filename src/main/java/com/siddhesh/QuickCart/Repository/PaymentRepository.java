package com.siddhesh.QuickCart.Repository;

import com.siddhesh.QuickCart.Entity.Order;
import com.siddhesh.QuickCart.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);
}
