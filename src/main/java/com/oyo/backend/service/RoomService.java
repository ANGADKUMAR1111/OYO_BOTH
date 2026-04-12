package com.oyo.backend.service;

import com.oyo.backend.entity.Room;
import com.oyo.backend.enums.RoomType;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    public List<Room> getHotelRooms(String hotelId, LocalDate checkIn, LocalDate checkOut) {
        if (!hotelRepository.existsById(hotelId))
            throw new ApiException("Hotel not found", HttpStatus.NOT_FOUND);
        if (checkIn != null && checkOut != null) {
            return roomRepository.findAvailableRooms(hotelId, checkIn, checkOut);
        }
        return roomRepository.findByHotelId(hotelId);
    }

    public Room getRoomById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Room createRoom(String hotelId, Map<String, Object> body) {
        if (!hotelRepository.existsById(hotelId))
            throw new ApiException("Hotel not found", HttpStatus.NOT_FOUND);
        Room room = Room.builder()
                .hotelId(hotelId)
                .roomType(RoomType.valueOf(((String) body.get("roomType")).toUpperCase()))
                .roomNumber((String) body.get("roomNumber"))
                .pricePerNight(((Number) body.get("pricePerNight")).doubleValue())
                .originalPrice(
                        body.get("originalPrice") != null ? ((Number) body.get("originalPrice")).doubleValue() : null)
                .maxOccupancy(((Number) body.get("maxOccupancy")).intValue())
                .description((String) body.get("description"))
                .bedType((String) body.getOrDefault("bedType", "Double Bed"))
                .amenities(body.get("amenities") != null ? (List<String>) body.get("amenities") : new ArrayList<>())
                .images(body.get("images") != null ? (List<String>) body.get("images") : new ArrayList<>())
                .isAvailable(true)
                .build();
        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(String roomId, Map<String, Object> body, String userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));
        if (body.get("pricePerNight") != null)
            room.setPricePerNight(((Number) body.get("pricePerNight")).doubleValue());
        if (body.get("maxOccupancy") != null)
            room.setMaxOccupancy(((Number) body.get("maxOccupancy")).intValue());
        if (body.get("description") != null)
            room.setDescription((String) body.get("description"));
        if (body.get("isAvailable") != null)
            room.setIsAvailable((Boolean) body.get("isAvailable"));
        if (body.get("amenities") != null)
            room.setAmenities((List<String>) body.get("amenities"));
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(String roomId) {
        if (!roomRepository.existsById(roomId))
            throw new ApiException("Room not found", HttpStatus.NOT_FOUND);
        roomRepository.deleteById(roomId);
    }
}
