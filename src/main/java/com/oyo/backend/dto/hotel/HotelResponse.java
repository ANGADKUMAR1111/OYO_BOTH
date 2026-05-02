package com.oyo.backend.dto.hotel;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
public class HotelResponse {
    private String id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private Integer starRating;
    private List<String> amenities;
    private List<String> images;
    private String hostId;
    private Boolean isApproved;
    private Boolean isFeatured;
    private Double averageRating;
    private Integer totalReviews;
    private Double minPrice;
    private Boolean isWishlisted;
    private Double distanceKm;
    private LocalDateTime createdAt;
}
