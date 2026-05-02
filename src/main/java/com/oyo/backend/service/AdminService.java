package com.oyo.backend.service;

import com.oyo.backend.repository.BookingRepository;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    @Cacheable(value = "adminStats", key = "'global'")
    public Map<String, Object> getStats() {
        long totalUsers = userRepository.count();
        long totalHotels = hotelRepository.countByIsApprovedTrue();
        long pendingApprovals = hotelRepository.countByIsApprovedFalse();
        long totalBookings = bookingRepository.count();
        Long paidBookings = bookingRepository.getPaidBookingsCount();
        Double totalRevenue = bookingRepository.getTotalRevenue();

        return Map.of(
                "totalUsers", totalUsers,
                "totalHotels", totalHotels,
                "pendingApprovals", pendingApprovals,
                "totalBookings", totalBookings,
                "paidBookings", paidBookings != null ? paidBookings : 0L,
                "totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
    }
}
