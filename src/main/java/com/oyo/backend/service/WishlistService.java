package com.oyo.backend.service;

import com.oyo.backend.dto.hotel.HotelResponse;
import com.oyo.backend.entity.Hotel;
import com.oyo.backend.entity.Wishlist;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.ReviewRepository;
import com.oyo.backend.repository.RoomRepository;
import com.oyo.backend.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public void addToWishlist(String userId, String hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ApiException("Hotel not found", HttpStatus.NOT_FOUND);
        }
        if (wishlistRepository.existsByUserIdAndHotelId(userId, hotelId)) {
            throw new ApiException("Hotel already in wishlist");
        }
        wishlistRepository.save(Wishlist.builder().userId(userId).hotelId(hotelId).build());
    }

    @Transactional
    public void removeFromWishlist(String userId, String hotelId) {
        wishlistRepository.deleteByUserIdAndHotelId(userId, hotelId);
    }

    public List<HotelResponse> getWishlist(String userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(w -> hotelRepository.findById(w.getHotelId()).orElse(null))
                .filter(h -> h != null)
                .map(h -> toHotelResponse(h, userId))
                .collect(Collectors.toList());
    }

    private HotelResponse toHotelResponse(Hotel hotel, String userId) {
        Double avgRating = reviewRepository.getAverageRating(hotel.getId());
        Long totalReviews = reviewRepository.getReviewCount(hotel.getId());
        Double minPrice = roomRepository.findByHotelId(hotel.getId()).stream()
                .mapToDouble(r -> r.getPricePerNight()).min().orElse(0.0);
        return HotelResponse.builder()
                .id(hotel.getId()).name(hotel.getName()).description(hotel.getDescription())
                .address(hotel.getAddress()).city(hotel.getCity()).state(hotel.getState())
                .country(hotel.getCountry()).latitude(hotel.getLatitude()).longitude(hotel.getLongitude())
                .starRating(hotel.getStarRating()).amenities(hotel.getAmenities()).images(hotel.getImages())
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalReviews(totalReviews != null ? totalReviews.intValue() : 0)
                .minPrice(minPrice).isWishlisted(true).build();
    }
}
