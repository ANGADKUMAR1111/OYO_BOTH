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

    private final com.oyo.backend.service.AdminService adminService;
    private final HotelRepository hotelRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = adminService.getStats();
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
