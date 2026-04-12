package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.entity.Room;
import com.oyo.backend.service.RoomService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management")
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<ApiResponse<List<Room>>> getHotelRooms(
            @PathVariable String hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getHotelRooms(hotelId, checkIn, checkOut)));
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<Room>> getRoom(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomById(id)));
    }

    @PostMapping("/hotels/{hotelId}/rooms")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<ApiResponse<Room>> createRoom(
            @PathVariable String hotelId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(roomService.createRoom(hotelId, body)));
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<ApiResponse<Room>> updateRoom(
            @PathVariable String id, @RequestBody Map<String, Object> body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(roomService.updateRoom(id, body, auth.getName())));
    }

    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteRoom(@PathVariable String id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted"));
    }
}
