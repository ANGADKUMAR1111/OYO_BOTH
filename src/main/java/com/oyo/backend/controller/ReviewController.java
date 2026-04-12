package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.entity.Review;
import com.oyo.backend.entity.User;
import com.oyo.backend.repository.UserRepository;
import com.oyo.backend.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Hotel reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    private String getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElseThrow();
    }

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<Review>> createReview(
            @RequestBody Map<String, Object> body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.createReview(getUserId(auth), body)));
    }

    @GetMapping("/hotels/{hotelId}/reviews")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getHotelReviews(
            @PathVariable String hotelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getHotelReviews(hotelId, page, size)));
    }

    @GetMapping("/reviews/user")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        String userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(reviewService.getUserReviews(userId, page, size)));
    }

    @PutMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<Review>> updateReview(
            @PathVariable String id, @RequestBody Map<String, Object> body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.updateReview(id, getUserId(auth),
                        body.get("rating") != null ? (Integer) body.get("rating") : null,
                        (String) body.get("comment"))));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<String>> deleteReview(@PathVariable String id, Authentication auth) {
        reviewService.deleteReview(id, getUserId(auth));
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }
}
