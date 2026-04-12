package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.entity.Coupon;
import com.oyo.backend.service.CouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Coupon management")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyCoupon(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        double amount = ((Number) body.get("amount")).doubleValue();
        return ResponseEntity.ok(ApiResponse.success(couponService.applyCoupon(code, amount)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<Coupon>>> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(couponService.getAllCoupons(page, size)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Coupon>> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(ApiResponse.success(couponService.createCoupon(coupon)));
    }
}
