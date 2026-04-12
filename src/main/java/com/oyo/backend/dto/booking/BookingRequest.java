package com.oyo.backend.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest {
    @NotBlank(message = "Hotel ID is required")
    private String hotelId;

    @NotBlank(message = "Room ID is required")
    private String roomId;

    @NotNull(message = "Check-in date is required")
    private LocalDate checkIn;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOut;

    @Positive(message = "Guests must be at least 1")
    private Integer guests = 1;

    private String couponCode;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String specialRequests;
}
