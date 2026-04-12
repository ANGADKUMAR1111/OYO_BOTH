package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.dto.payment.PaymentOrderRequest;
import com.oyo.backend.dto.payment.PaymentOrderResponse;
import com.oyo.backend.dto.payment.PaymentVerifyRequest;
import com.oyo.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment gateway")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(
            @RequestBody PaymentOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.createOrder(request)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request) {
        boolean valid = paymentService.verifyPayment(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(),
                request.getRazorpaySignature(), request.getBookingId());
        return ResponseEntity.ok(ApiResponse.success(valid ? "Payment verified" : "Payment verification failed"));
    }

    @PostMapping("/refund/{bookingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> refund(@PathVariable String bookingId) {
        String refundId = paymentService.refundPayment(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Refund initiated: " + refundId));
    }
}
