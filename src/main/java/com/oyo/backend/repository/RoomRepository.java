package com.oyo.backend.repository;

import com.oyo.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    List<Room> findByHotelId(String hotelId);

    List<Room> findByHotelIdAndIsAvailableTrue(String hotelId);

    /**
     * Returns rooms for a hotel that are NOT booked in the requested date range
     */
    @Query("SELECT r FROM Room r WHERE r.hotelId = :hotelId AND r.isAvailable = true AND r.id NOT IN (" +
            "  SELECT b.roomId FROM Booking b WHERE b.status IN ('PENDING','CONFIRMED') AND " +
            "  b.checkIn < :checkOut AND b.checkOut > :checkIn" +
            ")")
    List<Room> findAvailableRooms(@Param("hotelId") String hotelId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    boolean existsByHotelIdAndRoomNumber(String hotelId, String roomNumber);
}
