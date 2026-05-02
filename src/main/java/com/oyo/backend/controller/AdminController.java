package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.repository.BookingRepository;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard and management")
public class AdminController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    @GetMapping("/stats")
    @org.springframework.cache.annotation.Cacheable(value = "adminStats", key = "'global'")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() throws Exception {
        java.util.concurrent.CompletableFuture<Long> usersFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> userRepository.count());
        java.util.concurrent.CompletableFuture<Long> hotelsFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> hotelRepository.countByIsApprovedTrue());
        java.util.concurrent.CompletableFuture<Long> pendingFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> hotelRepository.countByIsApprovedFalse());
        java.util.concurrent.CompletableFuture<Long> bookingsFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> bookingRepository.count());
        java.util.concurrent.CompletableFuture<Long> paidFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> bookingRepository.getPaidBookingsCount());
        java.util.concurrent.CompletableFuture<Double> revenueFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> bookingRepository.getTotalRevenue());

        java.util.concurrent.CompletableFuture.allOf(usersFuture, hotelsFuture, pendingFuture, bookingsFuture, paidFuture, revenueFuture).join();

        Long paidBookings = paidFuture.get();
        Double totalRevenue = revenueFuture.get();

        Map<String, Object> stats = Map.of(
                "totalUsers", usersFuture.get(),
                "totalHotels", hotelsFuture.get(),
                "pendingApprovals", pendingFuture.get(),
                "totalBookings", bookingsFuture.get(),
                "paidBookings", paidBookings != null ? paidBookings : 0L,
                "totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/hotels/pending")
    public ResponseEntity<ApiResponse<?>> getPendingHotels() {
        return ResponseEntity.ok(ApiResponse.success(hotelRepository.findPendingHotels()));
    }

    @PutMapping("/hotels/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<String>> approveHotel(@PathVariable String id) {
        hotelRepository.findById(id).ifPresent(hotel -> {
            hotel.setIsApproved(true);
            hotelRepository.save(hotel);
        });
        return ResponseEntity.ok(ApiResponse.success("Hotel approved"));
    }

    @PutMapping("/hotels/{id}/featured")
    @Transactional
    public ResponseEntity<ApiResponse<String>> toggleFeatured(
            @PathVariable String id, @RequestBody Map<String, Boolean> body) {
        hotelRepository.findById(id).ifPresent(hotel -> {
            hotel.setIsFeatured(body.getOrDefault("featured", true));
            hotelRepository.save(hotel);
        });
        return ResponseEntity.ok(ApiResponse.success("Hotel featured status updated"));
    }
}
