package com.oyo.backend.service;

import com.oyo.backend.repository.BookingRepository;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    
    @Qualifier("applicationTaskExecutor")
    private final Executor taskExecutor;

    @Cacheable(value = "adminStats", key = "'global'")
    public Map<String, Object> getStats() throws Exception {
        CompletableFuture<Long> usersFuture = CompletableFuture.supplyAsync(() -> userRepository.count(), taskExecutor);
        CompletableFuture<Long> hotelsFuture = CompletableFuture.supplyAsync(() -> hotelRepository.countByIsApprovedTrue(), taskExecutor);
        CompletableFuture<Long> pendingFuture = CompletableFuture.supplyAsync(() -> hotelRepository.countByIsApprovedFalse(), taskExecutor);
        CompletableFuture<Long> bookingsFuture = CompletableFuture.supplyAsync(() -> bookingRepository.count(), taskExecutor);
        CompletableFuture<Long> paidFuture = CompletableFuture.supplyAsync(() -> bookingRepository.getPaidBookingsCount(), taskExecutor);
        CompletableFuture<Double> revenueFuture = CompletableFuture.supplyAsync(() -> bookingRepository.getTotalRevenue(), taskExecutor);

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
