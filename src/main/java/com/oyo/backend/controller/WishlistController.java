package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.dto.hotel.HotelResponse;
import com.oyo.backend.entity.User;
import com.oyo.backend.repository.UserRepository;
import com.oyo.backend.service.WishlistService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "User wishlist management")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    private String getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElseThrow();
    }

    @PostMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<String>> addToWishlist(@PathVariable String hotelId, Authentication auth) {
        wishlistService.addToWishlist(getUserId(auth), hotelId);
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist"));
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<String>> removeFromWishlist(@PathVariable String hotelId, Authentication auth) {
        wishlistService.removeFromWishlist(getUserId(auth), hotelId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelResponse>>> getWishlist(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getWishlist(getUserId(auth))));
    }
}
