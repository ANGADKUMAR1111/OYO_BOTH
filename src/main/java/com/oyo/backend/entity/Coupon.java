package com.oyo.backend.entity;

import com.oyo.backend.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    private String title;
    private String description;
    private String terms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private Double discountValue;

    private Double minBookingAmount;
    private Double maxDiscount;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private Integer usageLimit;

    @Builder.Default
    private Integer usedCount = 0;

    @Builder.Default
    private Boolean isActive = true;
}
