package com.oyo.backend.dto.payment;

import lombok.Data;

@Data
public class PaymentOrderRequest {
    private String bookingId;
    private Double amount;
    private String currency = "INR";
}
