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

    public Page<HotelResponse> searchHotels(String city, String query, Double minPrice, Double maxPrice,
            Integer rating, String amenities, String sort,
            String checkIn, String checkOut, Integer guests,
            int page, int size, String userId) {

        Sort sortOrder = buildSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        // Fetch paginated AND filtered directly from the database!
        Page<Hotel> hotels = hotelRepository.searchHotelsOptimized(query, city, minPrice, maxPrice, rating, pageable);

        List<HotelResponse> responseList = toResponseList(hotels.getContent(), userId, null);

        // Advanced local filtering for amenities only, since that is a comma-separated list
        // which varies structure heavily so usually handled locally (or via strict jsonb queries).
        if (amenities != null && !amenities.isBlank()) {
            List<String> required = Arrays.asList(amenities.split(","));
            responseList = responseList.stream()
                .filter(h -> h.getAmenities() != null && new HashSet<>(h.getAmenities()).containsAll(required))
                .collect(Collectors.toList());
        }

        return new PageImpl<>(responseList, pageable, hotels.getTotalElements());
    }

    public HotelResponse getHotelById(String id, String userId) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ApiException("Hotel not found", HttpStatus.NOT_FOUND));
        List<HotelResponse> result = toResponseList(List.of(hotel), userId, null);
        return result.get(0);
    }

    public Page<HotelResponse> getFeaturedHotels(int page, int size, String userId) {
        Pageable pageable = PageRequest.of(page, size);
        List<Hotel> hotels = hotelRepository.findByIsFeaturedTrueAndIsApprovedTrue(pageable).getContent();
        List<HotelResponse> responses = toResponseList(hotels, userId, null);
        return new PageImpl<>(responses, pageable,
                hotelRepository.findByIsFeaturedTrueAndIsApprovedTrue(pageable).getTotalElements());
    }

    public List<HotelResponse> getNearbyHotels(double lat, double lng, double radiusKm, String userId) {
        List<Hotel> allHotels = hotelRepository.findByIsApprovedTrue(PageRequest.of(0, 200)).getContent();

        // Compute distances first (cheap in-memory), then filter to the radius subset
        List<Hotel> inRadius = allHotels.stream()
                .filter(h -> h.getLatitude() != null && h.getLongitude() != null)
                .filter(h -> HaversineUtil.distance(lat, lng, h.getLatitude(), h.getLongitude()) <= radiusKm)
                .collect(Collectors.toList());

        // Build responses in bulk (no N+1)
        Map<String, Double> distances = new HashMap<>();
        for (Hotel h : inRadius) {
            distances.put(h.getId(), HaversineUtil.distance(lat, lng, h.getLatitude(), h.getLongitude()));
        }

        return toResponseList(inRadius, userId, distances)
                .stream()
                .sorted((a, b) -> Double.compare(
                        a.getDistanceKm() != null ? a.getDistanceKm() : Double.MAX_VALUE,
                        b.getDistanceKm() != null ? b.getDistanceKm() : Double.MAX_VALUE))
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
        Hotel saved = hotelRepository.save(hotel);
        return toResponseList(List.of(saved), hostId, null).get(0);
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
        if (request.getAmenities() != null) hotel.setAmenities(request.getAmenities());
        if (request.getImages() != null) hotel.setImages(request.getImages());
        Hotel saved = hotelRepository.save(hotel);
        return toResponseList(List.of(saved), userId, null).get(0);
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
        Hotel saved = hotelRepository.save(hotel);
        return toResponseList(List.of(saved), userId, null).get(0);
    }

    /** Cities list is stable — cache it in the JVM for the lifetime of the process. */
    @Cacheable("cities")
    public List<String> getCities() {
        return hotelRepository.findAllCities();
    }

    public List<String> searchCitySuggestions(String query) {
        return hotelRepository.searchCities(query);
    }

    // ── Core: bulk response builder (eliminates N+1) ──────────────────────

    /**
     * Converts a list of Hotel entities into HotelResponse DTOs using exactly:
     *   1 query for avg ratings (all hotels)
     *   1 query for review counts (all hotels)
     *   1 query for min prices  (all hotels)
     *   1 query for wishlist IDs (if userId != null)
     *
     * Total = 4 queries regardless of how many hotels are in the list.
     *
     * @param hotels     the hotels to convert
     * @param userId     current user id (may be null for unauthenticated requests)
     * @param distances  optional map of hotelId → distanceKm; null when not a nearby search
     */
    private List<HotelResponse> toResponseList(List<Hotel> hotels, String userId,
                                               Map<String, Double> distances) {
        if (hotels == null || hotels.isEmpty()) return Collections.emptyList();

        List<String> ids = hotels.stream().map(Hotel::getId).collect(Collectors.toList());

        // ── 1 query: average ratings ─────────────────────────────────────
        Map<String, Double> avgRatings = new HashMap<>();
        hotelRepository.findAverageRatingsForHotels(ids)
                .forEach(row -> avgRatings.put((String) row[0], toDouble(row[1])));

        // ── 1 query: review counts ───────────────────────────────────────
        Map<String, Long> reviewCounts = new HashMap<>();
        hotelRepository.findReviewCountsForHotels(ids)
                .forEach(row -> reviewCounts.put((String) row[0], toLong(row[1])));

        // ── 1 query: min room prices ─────────────────────────────────────
        Map<String, Double> minPrices = new HashMap<>();
        hotelRepository.findMinPricesForHotels(ids)
                .forEach(row -> minPrices.put((String) row[0], toDouble(row[1])));

        // ── 1 query: wishlisted hotel ids ────────────────────────────────
        Set<String> wishlistedIds = new HashSet<>();
        if (userId != null) {
            wishlistedIds.addAll(wishlistRepository.findHotelIdsByUserId(userId));
        }

        // ── Build responses in-memory (zero additional DB calls) ─────────
        List<HotelResponse> result = new ArrayList<>(hotels.size());
        for (Hotel hotel : hotels) {
            String id = hotel.getId();
            Double avg = avgRatings.getOrDefault(id, 0.0);
            double distKm = distances != null ? distances.getOrDefault(id, 0.0) : 0.0;

            result.add(HotelResponse.builder()
                    .id(id)
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
                    .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                    .totalReviews(reviewCounts.getOrDefault(id, 0L).intValue())
                    .minPrice(minPrices.getOrDefault(id, 0.0))
                    .isWishlisted(wishlistedIds.contains(id))
                    .distanceKm(distances != null
                            ? Math.round(distKm * 100.0) / 100.0
                            : null)
                    .createdAt(hotel.getCreatedAt())
                    .build());
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
