package com.oyo.backend.service;

import com.oyo.backend.dto.hotel.HotelRequest;
import com.oyo.backend.dto.hotel.HotelResponse;
import com.oyo.backend.entity.Hotel;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.ReviewRepository;
import com.oyo.backend.repository.RoomRepository;
import com.oyo.backend.repository.WishlistRepository;
import com.oyo.backend.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final WishlistRepository wishlistRepository;

    public Page<HotelResponse> searchHotels(String city, String query, Double minPrice, Double maxPrice,
            Integer rating, String amenities, String sort,
            String checkIn, String checkOut, Integer guests,
            int page, int size, String userId) {
        Sort sortOrder = buildSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<Hotel> hotels;
        if (query != null && !query.isBlank()) {
            hotels = hotelRepository.searchHotels(query, pageable);
        } else if (city != null && !city.isBlank()) {
            hotels = hotelRepository.findByCityIgnoreCaseAndIsApprovedTrue(city, pageable);
        } else {
            hotels = hotelRepository.findByIsApprovedTrue(pageable);
        }

        List<HotelResponse> responseList = hotels.getContent().stream()
                .map(h -> toResponse(h, userId, null))
                .filter(h -> filterByPrice(h, minPrice, maxPrice))
                .filter(h -> filterByRating(h, rating))
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, hotels.getTotalElements());
    }

    public HotelResponse getHotelById(String id, String userId) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ApiException("Hotel not found", HttpStatus.NOT_FOUND));
        return toResponse(hotel, userId, null);
    }

    public Page<HotelResponse> getFeaturedHotels(int page, int size, String userId) {
        Pageable pageable = PageRequest.of(page, size);
        return hotelRepository.findByIsFeaturedTrueAndIsApprovedTrue(pageable)
                .map(h -> toResponse(h, userId, null));
    }

    public List<HotelResponse> getNearbyHotels(double lat, double lng, double radiusKm, String userId) {
        List<Hotel> allHotels = hotelRepository.findByIsApprovedTrue(PageRequest.of(0, 200)).getContent();
        return allHotels.stream()
                .filter(h -> h.getLatitude() != null && h.getLongitude() != null)
                .map(h -> {
                    double dist = HaversineUtil.distance(lat, lng, h.getLatitude(), h.getLongitude());
                    return toResponse(h, userId, dist);
                })
                .filter(h -> h.getDistanceKm() != null && h.getDistanceKm() <= radiusKm)
                .sorted((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()))
                .collect(Collectors.toList());
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest request, String hostId) {
        Hotel hotel = Hotel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .starRating(request.getStarRating())
                .amenities(request.getAmenities() != null ? request.getAmenities() : new ArrayList<>())
                .images(request.getImages() != null ? request.getImages() : new ArrayList<>())
                .hostId(hostId)
                .isApproved(false)
                .isFeatured(false)
                .build();
        return toResponse(hotelRepository.save(hotel), hostId, null);
    }

    @Transactional
    public HotelResponse updateHotel(String hotelId, HotelRequest request, String userId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ApiException("Hotel not found", HttpStatus.NOT_FOUND));
        hotel.setName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setState(request.getState());
        hotel.setCountry(request.getCountry());
        hotel.setPincode(request.getPincode());
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());
        hotel.setStarRating(request.getStarRating());
        if (request.getAmenities() != null)
            hotel.setAmenities(request.getAmenities());
        if (request.getImages() != null)
            hotel.setImages(request.getImages());
        return toResponse(hotelRepository.save(hotel), userId, null);
    }

    public void deleteHotel(String hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ApiException("Hotel not found", HttpStatus.NOT_FOUND);
        }
        hotelRepository.deleteById(hotelId);
    }

    @Transactional
    public HotelResponse addImage(String hotelId, String imageUrl, String userId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ApiException("Hotel not found", HttpStatus.NOT_FOUND));
        hotel.getImages().add(imageUrl);
        return toResponse(hotelRepository.save(hotel), userId, null);
    }

    public List<String> getCities() {
        return hotelRepository.findAllCities();
    }

    public List<String> searchCitySuggestions(String query) {
        return hotelRepository.searchCities(query);
    }

    private HotelResponse toResponse(Hotel hotel, String userId, Double distanceKm) {
        Double avgRating = reviewRepository.getAverageRating(hotel.getId());
        Long totalReviews = reviewRepository.getReviewCount(hotel.getId());
        Double minPrice = roomRepository.findByHotelId(hotel.getId()).stream()
                .mapToDouble(r -> r.getPricePerNight())
                .min().orElse(0.0);
        boolean wishlisted = userId != null && wishlistRepository.existsByUserIdAndHotelId(userId, hotel.getId());

        return HotelResponse.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .description(hotel.getDescription())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .state(hotel.getState())
                .country(hotel.getCountry())
                .pincode(hotel.getPincode())
                .latitude(hotel.getLatitude())
                .longitude(hotel.getLongitude())
                .starRating(hotel.getStarRating())
                .amenities(hotel.getAmenities())
                .images(hotel.getImages())
                .hostId(hotel.getHostId())
                .isApproved(hotel.getIsApproved())
                .isFeatured(hotel.getIsFeatured())
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .totalReviews(totalReviews != null ? totalReviews.intValue() : 0)
                .minPrice(minPrice)
                .isWishlisted(wishlisted)
                .distanceKm(distanceKm != null ? Math.round(distanceKm * 100.0) / 100.0 : null)
                .createdAt(hotel.getCreatedAt())
                .build();
    }

    private Sort buildSort(String sort) {
        if (sort == null)
            return Sort.by("createdAt").descending();
        return switch (sort.toLowerCase()) {
            case "price_asc" -> Sort.by("createdAt").ascending();
            case "price_desc" -> Sort.by("createdAt").descending();
            case "rating" -> Sort.by("starRating").descending();
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by("createdAt").descending();
        };
    }

    private boolean filterByPrice(HotelResponse h, Double minPrice, Double maxPrice) {
        if (h.getMinPrice() == null)
            return true;
        if (minPrice != null && h.getMinPrice() < minPrice)
            return false;
        if (maxPrice != null && h.getMinPrice() > maxPrice)
            return false;
        return true;
    }

    private boolean filterByRating(HotelResponse h, Integer rating) {
        if (rating == null)
            return true;
        return h.getAverageRating() != null && h.getAverageRating() >= rating;
    }
}
