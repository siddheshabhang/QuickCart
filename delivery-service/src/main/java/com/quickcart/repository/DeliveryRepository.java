package com.quickcart.repository;

import com.quickcart.entiry.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Long, Delivery> {
    Optional<Delivery> findByOrderId(Long orderId);
}
