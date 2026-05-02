package com.oyo.backend.service;

import com.oyo.backend.repository.BookingRepository;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    @Cacheable(value = "adminStats", key = "'global'")
    public Map<String, Object> getStats() throws Exception {
        CompletableFuture<Long> usersFuture = CompletableFuture.supplyAsync(() -> userRepository.count());
        CompletableFuture<Long> hotelsFuture = CompletableFuture.supplyAsync(() -> hotelRepository.countByIsApprovedTrue());
        CompletableFuture<Long> pendingFuture = CompletableFuture.supplyAsync(() -> hotelRepository.countByIsApprovedFalse());
        CompletableFuture<Long> bookingsFuture = CompletableFuture.supplyAsync(() -> bookingRepository.count());
        CompletableFuture<Long> paidFuture = CompletableFuture.supplyAsync(() -> bookingRepository.getPaidBookingsCount());
        CompletableFuture<Double> revenueFuture = CompletableFuture.supplyAsync(() -> bookingRepository.getTotalRevenue());

        CompletableFuture.allOf(usersFuture, hotelsFuture, pendingFuture, bookingsFuture, paidFuture, revenueFuture).join();

        Long paidBookings = paidFuture.get();
        Double totalRevenue = revenueFuture.get();

        return Map.of(
                "totalUsers", usersFuture.get(),
                "totalHotels", hotelsFuture.get(),
                "pendingApprovals", pendingFuture.get(),
                "totalBookings", bookingsFuture.get(),
                "paidBookings", paidBookings != null ? paidBookings : 0L,
                "totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
    }
}
