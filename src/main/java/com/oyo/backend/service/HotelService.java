package com.oyo.backend.service;

import com.oyo.backend.dto.hotel.HotelRequest;
import com.oyo.backend.dto.hotel.HotelResponse;
import com.oyo.backend.entity.Hotel;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.WishlistRepository;
import com.oyo.backend.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HotelService — N+1-free implementation.
 *
 * OLD approach: for a page of N hotels → 4 DB queries per hotel = 4N extra queries.
 * NEW approach: 4 bulk queries total regardless of page size.
 *
 * For a 20-hotel page: 81 queries → 5 queries. ~16× fewer DB round-trips.
 */
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final WishlistRepository wishlistRepository;

    // ── Public API ────────────────────────────────────────────────────────

    @Lazy @Autowired private HotelService self;

    @Cacheable(value = "defaultHotels", 
               key = "'search:' + #page + ':' + #size + ':' + #city + ':' + #sort", 
               condition = "#query == null && #minPrice == null && #maxPrice == null && #rating == null && #amenities == null && #checkIn == null && #checkOut == null && #guests == null")
    public Page<HotelResponse> searchHotelsInternal(String city, String query, Double minPrice, Double maxPrice,
            Integer rating, String sort, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        Page<Object[]> rows = hotelRepository.searchHotelsOptimized(query, city, minPrice, maxPrice, rating, sort, pageable);
        
        List<HotelResponse> responses = rows.getContent().stream().map(row -> {
            Hotel hotel = (Hotel) row[0];
            Double minP = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            Double avgR = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            Integer revC = row[3] != null ? ((Number) row[3]).intValue() : 0;
            return buildBaseResponse(hotel, minP, avgR, revC, 0.0);
        }).collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, rows.getTotalElements());
    }

    public Page<HotelResponse> searchHotels(String city, String query, Double minPrice, Double maxPrice,
            Integer rating, String amenities, String sort,
            String checkIn, String checkOut, Integer guests,
            int page, int size, String userId) {

        Page<HotelResponse> cachedPage = self.searchHotelsInternal(city, query, minPrice, maxPrice, rating, sort, page, size);
        
        // Clone to avoid modifying cached objects
        List<HotelResponse> responseList = new ArrayList<>();
        for (HotelResponse r : cachedPage.getContent()) {
            responseList.add(r.toBuilder().build());
        }

        // Advanced local filtering for amenities
        if (amenities != null && !amenities.isBlank()) {
            List<String> required = Arrays.asList(amenities.split(","));
            responseList = responseList.stream()
                .filter(h -> h.getAmenities() != null && new HashSet<>(h.getAmenities()).containsAll(required))
                .collect(Collectors.toList());
        }

        injectWishlistStatus(responseList, userId);

        return new PageImpl<>(responseList, cachedPage.getPageable(), cachedPage.getTotalElements());
    }

    public HotelResponse getHotelById(String id, String userId) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ApiException("Hotel not found", HttpStatus.NOT_FOUND));
        List<HotelResponse> result = toResponseListLegacy(List.of(hotel), userId, null);
        return result.get(0);
    }

    @Cacheable(value = "featuredHotels", key = "'featured:' + #page + ':' + #size")
    public Page<HotelResponse> getFeaturedHotelsInternal(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        Page<Object[]> rows = hotelRepository.findFeaturedHotelsRaw(pageable);
        
        List<HotelResponse> responses = rows.getContent().stream().map(row -> {
            Hotel hotel = (Hotel) row[0];
            Double minP = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            Double avgR = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            Integer revC = row[3] != null ? ((Number) row[3]).intValue() : 0;
            return buildBaseResponse(hotel, minP, avgR, revC, 0.0);
        }).collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, rows.getTotalElements());
    }

    public Page<HotelResponse> getFeaturedHotels(int page, int size, String userId) {
        Page<HotelResponse> cachedPage = self.getFeaturedHotelsInternal(page, size);
        List<HotelResponse> responses = new ArrayList<>();
        for (HotelResponse r : cachedPage.getContent()) {
            responses.add(r.toBuilder().build());
        }
        injectWishlistStatus(responses, userId);
        return new PageImpl<>(responses, cachedPage.getPageable(), cachedPage.getTotalElements());
    }

    public List<HotelResponse> getNearbyHotels(double lat, double lng, double radiusKm, String userId) {
        List<Hotel> allHotels = hotelRepository.findByIsApprovedTrue(PageRequest.of(0, 200)).getContent();

        List<Hotel> inRadius = allHotels.stream()
                .filter(h -> h.getLatitude() != null && h.getLongitude() != null)
                .filter(h -> HaversineUtil.distance(lat, lng, h.getLatitude(), h.getLongitude()) <= radiusKm)
                .collect(Collectors.toList());

        Map<String, Double> distances = new HashMap<>();
        for (Hotel h : inRadius) {
            distances.put(h.getId(), HaversineUtil.distance(lat, lng, h.getLatitude(), h.getLongitude()));
        }

        return toResponseListLegacy(inRadius, userId, distances)
                .stream()
                .sorted((a, b) -> Double.compare(
                        a.getDistanceKm() != null ? a.getDistanceKm() : Double.MAX_VALUE,
                        b.getDistanceKm() != null ? b.getDistanceKm() : Double.MAX_VALUE))
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = { 
        @CacheEvict(value = "defaultHotels", allEntries = true), 
        @CacheEvict(value = "featuredHotels", allEntries = true),
        @CacheEvict(value = "cities", allEntries = true)
    })
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
        Hotel saved = hotelRepository.save(hotel);
        return toResponseListLegacy(List.of(saved), hostId, null).get(0);
    }

    @Transactional
    @Caching(evict = { 
        @CacheEvict(value = "defaultHotels", allEntries = true), 
        @CacheEvict(value = "featuredHotels", allEntries = true),
        @CacheEvict(value = "cities", allEntries = true)
    })
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
        if (request.getAmenities() != null) hotel.setAmenities(request.getAmenities());
        if (request.getImages() != null) hotel.setImages(request.getImages());
        Hotel saved = hotelRepository.save(hotel);
        return toResponseListLegacy(List.of(saved), userId, null).get(0);
    }

    @Caching(evict = { 
        @CacheEvict(value = "defaultHotels", allEntries = true), 
        @CacheEvict(value = "featuredHotels", allEntries = true),
        @CacheEvict(value = "cities", allEntries = true)
    })
    public void deleteHotel(String hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ApiException("Hotel not found", HttpStatus.NOT_FOUND);
        }
        hotelRepository.deleteById(hotelId);
    }

    @Transactional
    @Caching(evict = { 
        @CacheEvict(value = "defaultHotels", allEntries = true), 
        @CacheEvict(value = "featuredHotels", allEntries = true)
    })
    public HotelResponse addImage(String hotelId, String imageUrl, String userId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ApiException("Hotel not found", HttpStatus.NOT_FOUND));
        hotel.getImages().add(imageUrl);
        Hotel saved = hotelRepository.save(hotel);
        return toResponseListLegacy(List.of(saved), userId, null).get(0);
    }

    @Cacheable("cities")
    public List<String> getCities() {
        return hotelRepository.findAllCities();
    }

    public List<String> searchCitySuggestions(String query) {
        return hotelRepository.searchCities(query);
    }

    private void injectWishlistStatus(List<HotelResponse> responses, String userId) {
        if (userId == null || responses.isEmpty()) return;

        Set<String> hotelIds = responses.stream()
                .map(HotelResponse::getId)
                .collect(Collectors.toSet());

        Set<String> wishlisted = wishlistRepository.findWishlistedHotelIds(userId, hotelIds);
        responses.forEach(r -> r.setIsWishlisted(wishlisted.contains(r.getId())));
    }

    private HotelResponse buildBaseResponse(Hotel hotel, Double minPrice, Double avgRating, Integer totalReviews, Double distanceKm) {
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
                .totalReviews(totalReviews != null ? totalReviews : 0)
                .minPrice(minPrice != null ? minPrice : 0.0)
                .isWishlisted(false)
                .distanceKm(distanceKm != null && distanceKm > 0.0 ? Math.round(distanceKm * 100.0) / 100.0 : null)
                .createdAt(hotel.getCreatedAt())
                .build();
    }

    private List<HotelResponse> toResponseListLegacy(List<Hotel> hotels, String userId, Map<String, Double> distances) {
        if (hotels == null || hotels.isEmpty()) return Collections.emptyList();

        List<String> ids = hotels.stream().map(Hotel::getId).collect(Collectors.toList());

        Map<String, Double> avgRatings = new HashMap<>();
        hotelRepository.findAverageRatingsForHotels(ids).forEach(row -> avgRatings.put((String) row[0], toDouble(row[1])));

        Map<String, Long> reviewCounts = new HashMap<>();
        hotelRepository.findReviewCountsForHotels(ids).forEach(row -> reviewCounts.put((String) row[0], toLong(row[1])));

        Map<String, Double> minPrices = new HashMap<>();
        hotelRepository.findMinPricesForHotels(ids).forEach(row -> minPrices.put((String) row[0], toDouble(row[1])));

        Set<String> wishlistedIds = new HashSet<>();
        if (userId != null) wishlistedIds.addAll(wishlistRepository.findHotelIdsByUserId(userId));

        List<HotelResponse> result = new ArrayList<>(hotels.size());
        for (Hotel hotel : hotels) {
            String id = hotel.getId();
            result.add(buildBaseResponse(
                hotel, 
                minPrices.getOrDefault(id, 0.0), 
                avgRatings.getOrDefault(id, 0.0), 
                reviewCounts.getOrDefault(id, 0L).intValue(), 
                distances != null ? distances.getOrDefault(id, 0.0) : 0.0
            ).toBuilder().isWishlisted(wishlistedIds.contains(id)).build());
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Sort buildSort(String sort) {
        if (sort == null) return Sort.by("createdAt").descending();
        return switch (sort.toLowerCase()) {
            case "price_asc"  -> Sort.by("createdAt").ascending();
            case "price_desc" -> Sort.by("createdAt").descending();
            case "rating"     -> Sort.by("starRating").descending();
            case "newest"     -> Sort.by("createdAt").descending();
            default           -> Sort.by("createdAt").descending();
        };
    }

    private boolean filterByPrice(HotelResponse h, Double minPrice, Double maxPrice) {
        if (h.getMinPrice() == null) return true;
        if (minPrice != null && h.getMinPrice() < minPrice) return false;
        if (maxPrice != null && h.getMinPrice() > maxPrice) return false;
        return true;
    }

    private boolean filterByRating(HotelResponse h, Integer rating) {
        if (rating == null) return true;
        return h.getAverageRating() != null && h.getAverageRating() >= rating;
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double d) return d;
        if (value instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }
}
