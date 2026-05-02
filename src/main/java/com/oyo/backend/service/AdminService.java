package com.oyo.backend.service;

import com.oyo.backend.repository.BookingRepository;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalHotels", totalHotels);
        stats.put("pendingApprovals", pendingApprovals);
        stats.put("totalBookings", totalBookings);
        stats.put("paidBookings", paidBookings != null ? paidBookings : 0L);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        return stats;
    }
}
