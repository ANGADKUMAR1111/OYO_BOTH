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
            "(LOWER(h.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            " LOWER(h.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            " LOWER(h.address) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Hotel> searchHotels(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT h.city FROM Hotel h WHERE h.isApproved = true ORDER BY h.city")
    List<String> findAllCities();

    @Query("SELECT DISTINCT h.city FROM Hotel h WHERE h.isApproved = true AND LOWER(h.city) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> searchCities(@Param("query") String query);

    long countByIsApprovedFalse();

    long countByIsApprovedTrue();

    @Query("SELECT h FROM Hotel h WHERE h.isApproved = false ORDER BY h.createdAt DESC")
    List<Hotel> findPendingHotels();
}
