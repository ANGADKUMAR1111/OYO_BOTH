package com.oyo.backend.service;

import com.oyo.backend.entity.Review;
import com.oyo.backend.entity.User;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.BookingRepository;
import com.oyo.backend.repository.ReviewRepository;
import com.oyo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public Review createReview(String userId, Map<String, Object> body) {
        String hotelId = (String) body.get("hotelId");
        String bookingId = (String) body.get("bookingId");
        Integer rating = (Integer) body.get("rating");
        String comment = (String) body.get("comment");

        if (rating == null || rating < 1 || rating > 5) {
            throw new ApiException("Rating must be between 1 and 5");
        }
        if (bookingId != null && reviewRepository.existsByUserIdAndBookingId(userId, bookingId)) {
            throw new ApiException("You have already reviewed this booking");
        }

        Review review = Review.builder()
                .userId(userId)
                .hotelId(hotelId)
                .bookingId(bookingId)
                .rating(rating)
                .comment(comment)
                .build();
        return reviewRepository.save(review);
    }

    public Page<Map<String, Object>> getHotelReviews(String hotelId, int page, int size) {
        return reviewRepository.findByHotelIdOrderByCreatedAtDesc(hotelId, PageRequest.of(page, size))
                .map(review -> {
                    User user = userRepository.findById(review.getUserId()).orElse(null);
                    return Map.<String, Object>of(
                            "id", review.getId(),
                            "rating", review.getRating(),
                            "comment", review.getComment() != null ? review.getComment() : "",
                            "images", review.getImages(),
                            "createdAt", review.getCreatedAt().toString(),
                            "userName", user != null ? user.getName() : "Anonymous",
                            "userAvatar", user != null && user.getProfileImage() != null ? user.getProfileImage() : "");
                });
    }

    public Page<Map<String, Object>> getUserReviews(String userId, int page, int size) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(review -> {
                    User user = userRepository.findById(review.getUserId()).orElse(null);
                    return Map.<String, Object>of(
                            "id", review.getId(),
                            "hotelId", review.getHotelId() != null ? review.getHotelId() : "",
                            "rating", review.getRating(),
                            "comment", review.getComment() != null ? review.getComment() : "",
                            "images", review.getImages(),
                            "createdAt", review.getCreatedAt().toString(),
                            "userName", user != null ? user.getName() : "Anonymous",
                            "userAvatar", user != null && user.getProfileImage() != null ? user.getProfileImage() : "");
                });
    }

    @Transactional
    public Review updateReview(String reviewId, String userId, Integer rating, String comment) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException("Review not found", HttpStatus.NOT_FOUND));
        if (!review.getUserId().equals(userId)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        if (rating != null)
            review.setRating(rating);
        if (comment != null)
            review.setComment(comment);
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(String reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException("Review not found", HttpStatus.NOT_FOUND));
        if (!review.getUserId().equals(userId)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        reviewRepository.delete(review);
    }
}
