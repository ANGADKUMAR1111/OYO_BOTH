package com.oyo.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hotels", indexes = {
    @Index(name = "idx_hotel_city", columnList = "city"),
    @Index(name = "idx_hotel_is_approved", columnList = "isApproved"),
    @Index(name = "idx_hotel_is_featured", columnList = "isFeatured"),
    @Index(name = "idx_hotel_host_id", columnList = "hostId")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    private String state;
    private String country;
    private String pincode;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    private Integer starRating = 3;

    @ElementCollection
    @CollectionTable(name = "hotel_amenities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "amenity")
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "hotel_images", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "image_url")
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Column(nullable = false)
    private String hostId;

    @Builder.Default
    private Boolean isApproved = false;

    @Builder.Default
    private Boolean isFeatured = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Computed fields (not stored)
    @Transient
    private Double averageRating;

    @Transient
    private Integer totalReviews;

    @Transient
    private Double minPrice;
}
