package com.oyo.backend.repository;

import com.oyo.backend.entity.Booking;
import com.oyo.backend.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    Page<Booking> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Booking> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Booking> findByStatusAndCheckOutBefore(BookingStatus status, LocalDate date);

    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.paymentStatus = 'PAID'")
    Double getTotalRevenue();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.paymentStatus = 'PAID'")
    Long getPaidBookingsCount();

    boolean existsByUserIdAndHotelIdAndStatus(String userId, String hotelId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.status IN ('PENDING','CONFIRMED') AND " +
            "b.roomId = :roomId AND b.checkIn < :checkOut AND b.checkOut > :checkIn")
    List<Booking> findConflictingBookings(@Param("roomId") String roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    long countBy();
}
