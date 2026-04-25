package com.oyo.backend.repository;

import com.oyo.backend.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, String> {

    Page<Hotel> findByIsApprovedTrue(Pageable pageable);

    Page<Hotel> findByCityIgnoreCaseAndIsApprovedTrue(String city, Pageable pageable);

    Page<Hotel> findByIsFeaturedTrueAndIsApprovedTrue(Pageable pageable);

    Page<Hotel> findByHostId(String hostId, Pageable pageable);

    @Query("SELECT h FROM Hotel h WHERE h.isApproved = true AND " +
            "(:query IS NULL OR :query = '' OR LOWER(h.city) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR " +
            " LOWER(h.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR " +
            " LOWER(h.address) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) AND " +
            "(:city IS NULL OR :city = '' OR LOWER(h.city) = LOWER(:city)) AND " +
            "(:minP IS NULL OR (SELECT COALESCE(MIN(r.pricePerNight), 0) FROM Room r WHERE r.hotelId = h.id) >= :minP) AND " +
            "(:maxP IS NULL OR (SELECT COALESCE(MIN(r.pricePerNight), 999999) FROM Room r WHERE r.hotelId = h.id) <= :maxP) AND " +
            "(:minR IS NULL OR (SELECT COALESCE(AVG(rev.rating), 0.0) FROM Review rev WHERE rev.hotelId = h.id) >= :minR)")
    Page<Hotel> searchHotelsOptimized(
            @Param("query") String query,
            @Param("city") String city,
            @Param("minP") Double minPrice,
            @Param("maxP") Double maxPrice,
            @Param("minR") Integer rating,
            Pageable pageable);

    @Query("SELECT DISTINCT h.city FROM Hotel h WHERE h.isApproved = true ORDER BY h.city")
    List<String> findAllCities();

    @Query("SELECT DISTINCT h.city FROM Hotel h WHERE h.isApproved = true AND LOWER(h.city) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> searchCities(@Param("query") String query);

    long countByIsApprovedFalse();

    long countByIsApprovedTrue();

    @Query("SELECT h FROM Hotel h WHERE h.isApproved = false ORDER BY h.createdAt DESC")
    List<Hotel> findPendingHotels();

    // ── Bulk aggregate queries (eliminate N+1) ────────────────────────────

    /** Returns [hotelId, avgRating] for all hotels in the given id list. */
    @Query("SELECT r.hotelId, AVG(r.rating) FROM Review r WHERE r.hotelId IN :ids GROUP BY r.hotelId")
    List<Object[]> findAverageRatingsForHotels(@Param("ids") List<String> ids);

    /** Returns [hotelId, reviewCount] for all hotels in the given id list. */
    @Query("SELECT r.hotelId, COUNT(r) FROM Review r WHERE r.hotelId IN :ids GROUP BY r.hotelId")
    List<Object[]> findReviewCountsForHotels(@Param("ids") List<String> ids);

    /** Returns [hotelId, minPricePerNight] for all hotels in the given id list. */
    @Query("SELECT r.hotelId, MIN(r.pricePerNight) FROM Room r WHERE r.hotelId IN :ids GROUP BY r.hotelId")
    List<Object[]> findMinPricesForHotels(@Param("ids") List<String> ids);
}
