package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.dto.booking.BookingRequest;
import com.oyo.backend.dto.booking.BookingResponse;
import com.oyo.backend.entity.User;
import com.oyo.backend.enums.BookingStatus;
import com.oyo.backend.repository.UserRepository;
import com.oyo.backend.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    private String getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.createBooking(request, getUserId(auth))));
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getUserBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getUserBookings(getUserId(auth), page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(id, getUserId(auth))));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.cancelBooking(id, getUserId(auth))));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getAllBookings(page, size)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> updateStatus(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        BookingStatus status = BookingStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(bookingService.updateBookingStatus(id, status)));
    }
}
