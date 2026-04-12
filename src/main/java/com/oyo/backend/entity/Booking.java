package com.oyo.backend.entity;

import com.oyo.backend.enums.BookingStatus;
import com.oyo.backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String hotelId;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    @Builder.Default
    private Integer guests = 1;

    private Double roomPriceTotal;
    private Double taxAmount;
    private Double discountAmount;
    private Double totalAmount;

    private String couponCode;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String specialRequests;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String paymentId;
    private String razorpayOrderId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
