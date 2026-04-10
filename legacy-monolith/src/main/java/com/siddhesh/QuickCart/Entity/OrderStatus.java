package com.siddhesh.QuickCart.Entity;

public enum OrderStatus {
    CREATED,          // order placed
    PAYMENT_PENDING,  // payment initiated
    CONFIRMED,        // payment success
    ASSIGNED,         // delivery partner assigned
    OUT_FOR_DELIVERY, // partner picked up
    DELIVERED,        // completed
    FAILED,           // payment failed
    CANCELLED         // manually cancelled
}
