package com.siddhesh.QuickCart.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderAssignedEvent {
    private Long orderId;
    private Long userId;
    private String partnerName;
    private String partnerPhone;
}
