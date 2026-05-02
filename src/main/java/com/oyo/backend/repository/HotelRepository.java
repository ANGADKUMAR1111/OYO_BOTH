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

    @Query(value = """
        SELECT h,
               MIN(r.pricePerNight)  AS minPrice,
               AVG(rv.rating)        AS avgRating,
               COUNT(rv)             AS reviewCount
        FROM   Hotel h
               LEFT JOIN Room r ON r.hotelId = h.id
               LEFT JOIN Review rv ON rv.hotelId = h.id
        WHERE  (:query IS NULL OR :query = '' OR LOWER(h.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.address) LIKE LOWER(CONCAT('%', :query, '%')))
        AND    (:city IS NULL OR :city = '' OR LOWER(h.city) = LOWER(:city))
        AND    h.isApproved = true
        GROUP BY h
        HAVING (:minP IS NULL OR MIN(r.pricePerNight) >= :minP)
        AND    (:maxP IS NULL OR MIN(r.pricePerNight) <= :maxP)
        AND    (:minR IS NULL OR AVG(rv.rating) >= :minR)
        ORDER BY
            CASE WHEN :sortBy = 'price_asc'  THEN MIN(r.pricePerNight) END ASC,
            CASE WHEN :sortBy = 'price_desc' THEN MIN(r.pricePerNight) END DESC,
            CASE WHEN :sortBy = 'rating'     THEN AVG(rv.rating)       END DESC,
            h.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(h)
        FROM   Hotel h
               LEFT JOIN Room r ON r.hotelId = h.id
               LEFT JOIN Review rv ON rv.hotelId = h.id
        WHERE  (:query IS NULL OR :query = '' OR LOWER(h.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.address) LIKE LOWER(CONCAT('%', :query, '%')))
        AND    (:city IS NULL OR :city = '' OR LOWER(h.city) = LOWER(:city))
        AND    h.isApproved = true
        GROUP BY h
        HAVING (:minP IS NULL OR MIN(r.pricePerNight) >= :minP)
        AND    (:maxP IS NULL OR MIN(r.pricePerNight) <= :maxP)
        AND    (:minR IS NULL OR AVG(rv.rating) >= :minR)
        """)
    Page<Object[]> searchHotelsOptimized(
            @Param("query") String query,
            @Param("city") String city,
            @Param("minP") Double minPrice,
            @Param("maxP") Double maxPrice,
            @Param("minR") Integer rating,
            @Param("sortBy") String sortBy,
            Pageable pageable);

    @Query(value = """
        SELECT h,
               MIN(r.pricePerNight) AS minPrice,
               AVG(rv.rating)       AS avgRating,
               COUNT(rv)            AS reviewCount
        FROM   Hotel h
               LEFT JOIN Room r ON r.hotelId = h.id
               LEFT JOIN Review rv ON rv.hotelId = h.id
        WHERE  h.isApproved = true
        AND    h.isFeatured = true
        GROUP BY h
        ORDER BY AVG(rv.rating) DESC, COUNT(rv) DESC
        """,
        countQuery = """
        SELECT COUNT(h)
        FROM   Hotel h
        WHERE  h.isApproved = true
        AND    h.isFeatured = true
        """)
    Page<Object[]> findFeaturedHotelsRaw(Pageable pageable);

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
