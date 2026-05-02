package com.oyo.backend.entity;

import com.oyo.backend.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms", indexes = {
    @Index(name = "idx_room_hotel_id", columnList = "hotelId"),
    @Index(name = "idx_room_price_per_night", columnList = "pricePerNight")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String hotelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType;

    private String roomNumber;

    @Column(nullable = false)
    private Double pricePerNight;

    private Double originalPrice;

    @Column(nullable = false)
    private Integer maxOccupancy;

    private String description;

    private String bedType;

    @ElementCollection
    @CollectionTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "amenity")
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "room_images", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "image_url")
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Builder.Default
    private Boolean isAvailable = true;
}
