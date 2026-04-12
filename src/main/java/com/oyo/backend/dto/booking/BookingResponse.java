package com.oyo.backend.dto.booking;

import com.oyo.backend.enums.BookingStatus;
import com.oyo.backend.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private String id;
    private String hotelId;
    private String hotelName;
    private String hotelImage;
    private String hotelCity;
    private String roomId;
    private String roomType;
    private String roomNumber;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer nights;
    private Integer guests;
    private Double roomPriceTotal;
    private Double taxAmount;
    private Double discountAmount;
    private Double totalAmount;
    private String couponCode;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String specialRequests;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private String paymentId;
    private String razorpayOrderId;
    private LocalDateTime createdAt;
}
