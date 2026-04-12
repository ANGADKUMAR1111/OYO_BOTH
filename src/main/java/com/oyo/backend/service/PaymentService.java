package com.oyo.backend.service;

import com.oyo.backend.dto.payment.PaymentOrderRequest;
import com.oyo.backend.dto.payment.PaymentOrderResponse;
import com.oyo.backend.entity.Booking;
import com.oyo.backend.enums.BookingStatus;
import com.oyo.backend.enums.NotificationType;
import com.oyo.backend.enums.PaymentStatus;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.BookingRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public PaymentOrderResponse createOrder(PaymentOrderRequest request) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject options = new JSONObject();
            // Razorpay amount is in paise (multiply by 100)
            options.put("amount", (int) (request.getAmount() * 100));
            options.put("currency", request.getCurrency() != null ? request.getCurrency() : "INR");
            options.put("receipt", "booking_" + request.getBookingId());
            options.put("payment_capture", 1);

            Order order = client.orders.create(options);
            String orderId = order.get("id");

            // Save order ID to booking
            if (request.getBookingId() != null) {
                bookingRepository.findById(request.getBookingId()).ifPresent(booking -> {
                    booking.setRazorpayOrderId(orderId);
                    bookingRepository.save(booking);
                });
            }

            return PaymentOrderResponse.builder()
                    .orderId(orderId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .keyId(razorpayKeyId)
                    .bookingId(request.getBookingId())
                    .description("OYO Room Booking")
                    .build();
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new ApiException("Payment order creation failed: " + e.getMessage());
        }
    }

    @Transactional
    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature, String bookingId) {
        String signatureData = razorpayOrderId + "|" + razorpayPaymentId;
        boolean isValid = verifySignature(signatureData, razorpaySignature);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));

        if (isValid) {
            booking.setPaymentStatus(PaymentStatus.PAID);
            booking.setPaymentId(razorpayPaymentId);
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            notificationService.createNotification(booking.getUserId(),
                    "Payment Successful ✅",
                    "Payment of ₹" + booking.getTotalAmount() + " received. Booking confirmed!",
                    NotificationType.PAYMENT, bookingId);
        } else {
            booking.setPaymentStatus(PaymentStatus.FAILED);
            bookingRepository.save(booking);
            log.warn("Payment signature verification failed for booking: {}", bookingId);
        }
        return isValid;
    }

    @Transactional
    public String refundPayment(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));

        if (booking.getPaymentId() == null) {
            throw new ApiException("No payment found for this booking");
        }

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject refundOptions = new JSONObject();
            refundOptions.put("amount", (int) (booking.getTotalAmount() * 100));
            Refund refund = client.payments.refund(booking.getPaymentId(), refundOptions);

            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            notificationService.createNotification(booking.getUserId(),
                    "Refund Initiated 💰",
                    "Refund of ₹" + booking.getTotalAmount()
                            + " has been initiated. It will reflect in 5-7 business days.",
                    NotificationType.PAYMENT, bookingId);

            return refund.get("id");
        } catch (RazorpayException e) {
            log.error("Refund failed: {}", e.getMessage());
            throw new ApiException("Refund failed: " + e.getMessage());
        }
    }

    private boolean verifySignature(String data, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
