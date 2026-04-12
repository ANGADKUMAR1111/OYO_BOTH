package com.oyo.backend.repository;

import com.oyo.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    Page<Review> findByHotelIdOrderByCreatedAtDesc(String hotelId, Pageable pageable);

    Page<Review> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    boolean existsByUserIdAndBookingId(String userId, String bookingId);

    Optional<Review> findByUserIdAndHotelId(String userId, String hotelId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.hotelId = :hotelId")
    Double getAverageRating(@Param("hotelId") String hotelId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.hotelId = :hotelId")
    Long getReviewCount(@Param("hotelId") String hotelId);
}
