package com.oyo.backend.service;

import com.oyo.backend.entity.Coupon;
import com.oyo.backend.enums.DiscountType;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public Map<String, Object> applyCoupon(String code, double bookingAmount) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ApiException("Coupon code not found"));

        if (!coupon.getIsActive())
            throw new ApiException("Coupon is not active");
        if (coupon.getExpiryDate().isBefore(LocalDateTime.now()))
            throw new ApiException("Coupon has expired");
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit())
            throw new ApiException("Coupon usage limit reached");
        if (coupon.getMinBookingAmount() != null && bookingAmount < coupon.getMinBookingAmount())
            throw new ApiException("Minimum booking amount is ₹" + coupon.getMinBookingAmount());

        double discount;
        if (coupon.getDiscountType() == DiscountType.FLAT) {
            discount = coupon.getDiscountValue();
        } else {
            discount = bookingAmount * (coupon.getDiscountValue() / 100.0);
            if (coupon.getMaxDiscount() != null)
                discount = Math.min(discount, coupon.getMaxDiscount());
        }

        return Map.of(
                "code", code,
                "discountType", coupon.getDiscountType().name(),
                "discountValue", coupon.getDiscountValue(),
                "discountAmount", discount,
                "finalAmount", Math.max(0, bookingAmount - discount),
                "message", "Coupon applied! You save ₹" + discount);
    }

    public Page<Coupon> getAllCoupons(int page, int size) {
        return couponRepository.findAll(PageRequest.of(page, size));
    }

    public Coupon createCoupon(Coupon coupon) {
        if (couponRepository.existsByCodeIgnoreCase(coupon.getCode())) {
            throw new ApiException("Coupon code already exists");
        }
        return couponRepository.save(coupon);
    }
}
