package com.siddhesh.QuickCart.Repository;

import com.siddhesh.QuickCart.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
