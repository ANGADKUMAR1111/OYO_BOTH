package com.oyo.backend.repository;

import com.oyo.backend.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, String> {
    List<Wishlist> findByUserId(String userId);

    Optional<Wishlist> findByUserIdAndHotelId(String userId, String hotelId);

    boolean existsByUserIdAndHotelId(String userId, String hotelId);

    void deleteByUserIdAndHotelId(String userId, String hotelId);

    /** Fetch all wishlisted hotel IDs for a user in one query — eliminates N wishlist lookups. */
    @Query("SELECT w.hotelId FROM Wishlist w WHERE w.userId = :userId")
    List<String> findHotelIdsByUserId(@Param("userId") String userId);

    @Query("SELECT w.hotelId FROM Wishlist w WHERE w.userId = :userId AND w.hotelId IN :hotelIds")
    Set<String> findWishlistedHotelIds(@Param("userId") String userId, @Param("hotelIds") Set<String> hotelIds);
}
