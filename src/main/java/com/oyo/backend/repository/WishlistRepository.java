package com.oyo.backend.repository;

import com.oyo.backend.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, String> {
    List<Wishlist> findByUserId(String userId);

    Optional<Wishlist> findByUserIdAndHotelId(String userId, String hotelId);

    boolean existsByUserIdAndHotelId(String userId, String hotelId);

    void deleteByUserIdAndHotelId(String userId, String hotelId);
}
