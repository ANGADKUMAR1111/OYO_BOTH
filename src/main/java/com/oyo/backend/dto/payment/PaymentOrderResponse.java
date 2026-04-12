package com.oyo.backend.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentOrderResponse {
    private String orderId;
    private Double amount;
    private String currency;
    private String keyId;
    private String bookingId;
    private String description;
}
