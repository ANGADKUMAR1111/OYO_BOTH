package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.dto.hotel.HotelRequest;
import com.oyo.backend.dto.hotel.HotelResponse;
import com.oyo.backend.entity.User;
import com.oyo.backend.repository.UserRepository;
import com.oyo.backend.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotels", description = "Hotel search and management")
public class HotelController {

    private final HotelService hotelService;
    private final UserRepository userRepository;

    private String getUserId(Authentication auth) {
        if (auth == null)
            return null;
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }

    @GetMapping
    @Operation(summary = "Search hotels with filters")
    public ResponseEntity<ApiResponse<Page<HotelResponse>>> searchHotels(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String amenities,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            @RequestParam(required = false) Integer guests,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                hotelService.searchHotels(city, query, minPrice, maxPrice, rating, amenities, sort,
                        checkIn, checkOut, guests, page, size, getUserId(auth))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponse>> getHotel(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.getHotelById(id, getUserId(auth))));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<Page<HotelResponse>>> getFeatured(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.getFeaturedHotels(page, size, getUserId(auth))));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<HotelResponse>>> getNearby(
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.getNearbyHotels(lat, lng, radius, getUserId(auth))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<ApiResponse<HotelResponse>> createHotel(
            @Valid @RequestBody HotelRequest request, Authentication auth) {
        String userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(hotelService.createHotel(request, userId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<ApiResponse<HotelResponse>> updateHotel(
            @PathVariable String id, @Valid @RequestBody HotelRequest request, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.updateHotel(id, request, getUserId(auth))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteHotel(@PathVariable String id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel deleted successfully"));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<ApiResponse<HotelResponse>> addImage(
            @PathVariable String id, @RequestBody Map<String, String> body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                hotelService.addImage(id, body.get("imageUrl"), getUserId(auth))));
    }
}
