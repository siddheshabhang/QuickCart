package com.quickcart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String transactionId;

    private LocalDateTime createdAt;

    @Column(unique = true)
    private String idempotencyKey;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
