package com.oyo.backend.config;

import com.oyo.backend.entity.*;
import com.oyo.backend.enums.*;
import com.oyo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DataSeeder — runs once on startup.
 * Skips seeding if hotels already exist (idempotent).
 *
 * Seeds:
 * • 1 admin + 5 regular users
 * • 20 hotels across 8 Indian cities
 * • 3–4 room types per hotel
 * • Past & upcoming bookings
 * • 2–3 reviews per hotel
 * • 6 discount coupons
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

        private final UserRepository userRepository;
        private final HotelRepository hotelRepository;
        private final RoomRepository roomRepository;
        private final BookingRepository bookingRepository;
        private final ReviewRepository reviewRepository;
        private final CouponRepository couponRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(ApplicationArguments args) {
                if (hotelRepository.count() > 0) {
                        log.info("DataSeeder: data already present — skipping.");
                        return;
                }
                log.info("DataSeeder: seeding database…");

                List<User> users = seedUsers();
                List<Hotel> hotels = seedHotels(users.get(0).getId()); // admin as host
                seedRooms(hotels);
                seedBookings(users, hotels);
                seedReviews(users, hotels);
                seedCoupons();

                log.info("DataSeeder: done. {} users, {} hotels seeded.",
                                users.size(), hotels.size());
        }

        // ─────────────────────────── USERS ──────────────────────────────────────

        private List<User> seedUsers() {
                String pw = passwordEncoder.encode("Password@123");

                User admin = User.builder()
                                .name("Admin OYO")
                                .email("admin@oyorooms.com")
                                .phone("+919000000001")
                                .password(pw)
                                .role(UserRole.ADMIN)
                                .isVerified(true)
                                .build();

                List<User> regularUsers = List.of(
                                User.builder().name("Rahul Sharma").email("rahul.sharma@gmail.com")
                                                .phone("+919876543210").password(pw).role(UserRole.USER)
                                                .isVerified(true).build(),
                                User.builder().name("Priya Nair").email("priya.nair@gmail.com")
                                                .phone("+919876543211").password(pw).role(UserRole.USER)
                                                .isVerified(true).build(),
                                User.builder().name("Amit Patel").email("amit.patel@gmail.com")
                                                .phone("+919876543212").password(pw).role(UserRole.USER)
                                                .isVerified(true).build(),
                                User.builder().name("Sneha Kapoor").email("sneha.kapoor@gmail.com")
                                                .phone("+919876543213").password(pw).role(UserRole.USER)
                                                .isVerified(true).build(),
                                User.builder().name("Vikram Singh").email("vikram.singh@gmail.com")
                                                .phone("+919876543214").password(pw).role(UserRole.USER)
                                                .isVerified(true).build());

                List<User> all = new ArrayList<>();
                all.add(userRepository.save(admin));
                regularUsers.forEach(u -> all.add(userRepository.save(u)));
                return all;
        }

        // ─────────────────────────── HOTELS ─────────────────────────────────────

        private List<Hotel> seedHotels(String hostId) {
                List<Hotel> hotels = new ArrayList<>();

                // ── Mumbai ──────────────────────────────────────────────
                hotels.add(hotel(hostId,
                                "OYO Flagship — Bandra Heights",
                                "A premium stay in the heart of Bandra with modern amenities and stunning sea-facing views.",
                                "3rd Road, Khar West, Bandra", "Mumbai", "Maharashtra", "400050",
                                19.0596, 72.8295, 4,
                                amenities("Free WiFi", "AC", "Flat-screen TV", "Room Service", "Swimming Pool", "Gym",
                                                "Breakfast Included"),
                                hotelImages("mumbai", 1)));

                hotels.add(hotel(hostId,
                                "OYO Townhouse — Andheri West",
                                "Your home away from home in Andheri, close to the airport and entertainment hubs.",
                                "Lokhandwala Complex, Andheri West", "Mumbai", "Maharashtra", "400053",
                                19.1255, 72.8337, 3,
                                amenities("Free WiFi", "AC", "Flat-screen TV", "24/7 Reception", "Laundry"),
                                hotelImages("mumbai", 2)));

                // ── Delhi ────────────────────────────────────────────────
                hotels.add(hotel(hostId,
                                "OYO Premium — Connaught Place",
                                "Classic heritage hotel steps away from historic Connaught Place and Rajiv Chowk metro.",
                                "Block F, Connaught Place", "Delhi", "Delhi", "110001",
                                28.6328, 77.2197, 5,
                                amenities("Free WiFi", "AC", "Restaurant", "Bar", "Business Centre", "Spa",
                                                "Valet Parking"),
                                hotelImages("delhi", 1)));

                hotels.add(hotel(hostId,
                                "OYO Collection O — Karol Bagh",
                                "Budget-friendly comfort in Karol Bagh with easy metro access.",
                                "Arya Samaj Road, Karol Bagh", "Delhi", "Delhi", "110005",
                                28.6520, 77.1884, 3,
                                amenities("Free WiFi", "AC", "Flat-screen TV", "Power Backup", "CCTV"),
                                hotelImages("delhi", 2)));

                hotels.add(hotel(hostId,
                                "OYO Home — Saket",
                                "Peaceful, residential-style stay in upscale Saket near Select Citywalk Mall.",
                                "Press Enclave Road, Saket", "Delhi", "Delhi", "110017",
                                28.5244, 77.2167, 3,
                                amenities("Free WiFi", "AC", "Kitchenette", "Parking", "Hot Water"),
                                hotelImages("delhi", 3)));

                // ── Bangalore ────────────────────────────────────────────
                hotels.add(hotel(hostId,
                                "OYO Flagship — Indiranagar",
                                "Trendy boutique hotel in Indiranagar, Bangalore's hippest neighbourhood.",
                                "100 Feet Road, Indiranagar", "Bangalore", "Karnataka", "560038",
                                12.9784, 77.6408, 4,
                                amenities("Free WiFi", "AC", "Rooftop Café", "Gym", "Electric Vehicle Charging",
                                                "Laundry"),
                                hotelImages("bangalore", 1)));

                hotels.add(hotel(hostId,
                                "OYO Townhouse — Koramangala",
                                "Modern co-living vibe hotel in Koramangala, tech hub of Bangalore.",
                                "5th Block, Koramangala", "Bangalore", "Karnataka", "560095",
                                12.9352, 77.6245, 3,
                                amenities("Free WiFi", "AC", "Co-Working Space", "Café", "Power Backup"),
                                hotelImages("bangalore", 2)));

                hotels.add(hotel(hostId,
                                "OYO Palette — Whitefield",
                                "Contemporary hotel near ITPL and international tech parks.",
                                "Whitefield Main Road", "Bangalore", "Karnataka", "560066",
                                12.9698, 77.7499, 4,
                                amenities("Free WiFi", "AC", "Swimming Pool", "Gym", "Restaurant", "Bar"),
                                hotelImages("bangalore", 3)));

                // ── Goa ─────────────────────────────────────────────────
                hotels.add(hotel(hostId,
                                "OYO Resort — Calangute Beach",
                                "Beachside resort with direct access to the golden sands of Calangute.",
                                "Calangute Beach Road, North Goa", "Goa", "Goa", "403516",
                                15.5449, 73.7523, 4,
                                amenities("Free WiFi", "AC", "Beach Access", "Swimming Pool", "Restaurant",
                                                "Water Sports", "Spa"),
                                hotelImages("goa", 1)));

                hotels.add(hotel(hostId,
                                "OYO Rooms — Baga Road",
                                "Affordable cozy rooms walking distance from the famous Baga Beach.",
                                "Baga Road, North Goa", "Goa", "Goa", "403516",
                                15.5576, 73.7540, 3,
                                amenities("Free WiFi", "AC", "Outdoor Seating", "Bicycle Rental", "Housekeeping"),
                                hotelImages("goa", 2)));

                // ── Jaipur ──────────────────────────────────────────────
                hotels.add(hotel(hostId,
                                "OYO Heritage — Pink City Haveli",
                                "Opulent palace-style haveli in the heart of the Pink City with royal décor.",
                                "Johari Bazaar Road, Old City", "Jaipur", "Rajasthan", "302001",
                                26.9185, 75.8282, 5,
                                amenities("Free WiFi", "AC", "Heritage Pool", "Rooftop Restaurant", "Cultural Events",
                                                "Guided City Tours"),
                                hotelImages("jaipur", 1)));

                hotels.add(hotel(hostId,
                                "OYO Flagship — C-Scheme",
                                "Contemporary hotel in Jaipur's upscale C-Scheme locality.",
                                "Panch Batti, C-Scheme", "Jaipur", "Rajasthan", "302001",
                                26.9098, 75.8062, 4,
                                amenities("Free WiFi", "AC", "Gym", "Business Centre", "24/7 Room Service"),
                                hotelImages("jaipur", 2)));

                // ── Hyderabad ────────────────────────────────────────────
                hotels.add(hotel(hostId,
                                "OYO Premium — HITEC City",
                                "Sleek tech-friendly hotel in HITEC City, minutes from Cyberabad offices.",
                                "Mindspace Road, HITEC City", "Hyderabad", "Telangana", "500081",
                                17.4435, 78.3772, 4,
                                amenities("Free WiFi", "AC", "Co-Working Lounge", "Gym", "Restaurant",
                                                "Airport Shuttle"),
                                hotelImages("hyderabad", 1)));

                hotels.add(hotel(hostId,
                                "OYO Townhouse — Banjara Hills",
                                "Stylish boutique hotel in Hyderabad's trendy Banjara Hills.",
                                "Road No. 12, Banjara Hills", "Hyderabad", "Telangana", "500034",
                                17.4126, 78.4479, 4,
                                amenities("Free WiFi", "AC", "Rooftop Bar", "Restaurant", "Valet Parking"),
                                hotelImages("hyderabad", 2)));

                // ── Chennai ──────────────────────────────────────────────
                hotels.add(hotel(hostId,
                                "OYO Flagship — Anna Nagar",
                                "Well-located hotel in Anna Nagar with excellent connectivity across Chennai.",
                                "2nd Avenue, Anna Nagar", "Chennai", "Tamil Nadu", "600040",
                                13.0878, 80.2123, 3,
                                amenities("Free WiFi", "AC", "Restaurant", "24/7 Reception", "Laundry"),
                                hotelImages("chennai", 1)));

                hotels.add(hotel(hostId,
                                "OYO Collection O — T. Nagar",
                                "Budget comfort hotel in T. Nagar, Chennai's prime shopping district.",
                                "Usman Road, T. Nagar", "Chennai", "Tamil Nadu", "600017",
                                13.0392, 80.2324, 3,
                                amenities("Free WiFi", "AC", "Flat-screen TV", "Power Backup", "CCTV"),
                                hotelImages("chennai", 2)));

                // ── Pune ─────────────────────────────────────────────────
                hotels.add(hotel(hostId,
                                "OYO Premium — Koregaon Park",
                                "Chic boutique hotel in Pune's lively Koregaon Park neighbourhood.",
                                "Lane 6, Koregaon Park", "Pune", "Maharashtra", "411001",
                                18.5362, 73.8930, 4,
                                amenities("Free WiFi", "AC", "Café", "Outdoor Lounge", "Spa", "Gym"),
                                hotelImages("pune", 1)));

                hotels.add(hotel(hostId,
                                "OYO Townhouse — Hinjewadi",
                                "Tech-park adjacent hotel ideal for business travellers near Hinjewadi IT Park.",
                                "Phase 1, Hinjewadi", "Pune", "Maharashtra", "411057",
                                18.5912, 73.7389, 3,
                                amenities("Free WiFi", "AC", "Conference Room", "Shuttle Service", "24/7 Room Service"),
                                hotelImages("pune", 2)));

                hotels.add(hotel(hostId,
                                "OYO Home — Kothrud",
                                "Peaceful residential stay in Pune's Kothrud with a home-like atmosphere.",
                                "Paud Road, Kothrud", "Pune", "Maharashtra", "411038",
                                18.5013, 73.8067, 3,
                                amenities("Free WiFi", "AC", "Kitchenette", "Parking", "Hot Water",
                                                "Daily Housekeeping"),
                                hotelImages("pune", 3)));

                return hotelRepository.saveAll(hotels);
        }

        // ─────────────────────────── ROOMS ──────────────────────────────────────

        private void seedRooms(List<Hotel> hotels) {
                List<Room> rooms = new ArrayList<>();
                Random rng = new Random(42);

                for (Hotel h : hotels) {
                        // Each hotel gets 3-4 room types: SINGLE, DOUBLE, DELUXE, (maybe SUITE)
                        boolean hasSuite = rng.nextBoolean();

                        double base = 800 + rng.nextInt(700) * (double) h.getStarRating();

                        rooms.add(room(h.getId(), RoomType.SINGLE, "101", base * 0.7, base * 0.9, 1,
                                        "Cosy single room ideal for solo travellers with all modern amenities.",
                                        "Single Bed",
                                        amenities("Free WiFi", "AC", "Flat-screen TV", "Hot Water", "Safe")));

                        rooms.add(room(h.getId(), RoomType.DOUBLE, "201", base, base * 1.25, 2,
                                        "Spacious double room with twin or king-size bed options.", "Double/Twin Bed",
                                        amenities("Free WiFi", "AC", "Flat-screen TV", "Hot Water", "Safe",
                                                        "Mini Fridge")));

                        rooms.add(room(h.getId(), RoomType.DELUXE, "301", base * 1.5, base * 2.0, 3,
                                        "Premium Deluxe room with city view, work desk and upgraded bath.", "King Bed",
                                        amenities("Free WiFi", "AC", "Flat-screen TV", "Bathtub", "Mini Bar",
                                                        "Room Service",
                                                        "City View")));

                        if (hasSuite) {
                                rooms.add(room(h.getId(), RoomType.SUITE, "401", base * 2.5, base * 3.2, 4,
                                                "Luxurious suite with separate living area, premium amenities and panoramic view.",
                                                "Super King Bed",
                                                amenities("Free WiFi", "AC", "Smart TV", "Jacuzzi", "Mini Bar",
                                                                "Butler Service", "Lounge Area",
                                                                "Balcony")));
                        }
                }

                roomRepository.saveAll(rooms);
        }

        // ─────────────────────────── BOOKINGS ───────────────────────────────────

        private void seedBookings(List<User> users, List<Hotel> hotels) {
                List<Booking> bookings = new ArrayList<>();
                List<Room> allRooms = roomRepository.findAll();
                Random rng = new Random(42);

                // Map hotel → its rooms
                Map<String, List<Room>> hotelRooms = new HashMap<>();
                for (Room r : allRooms) {
                        hotelRooms.computeIfAbsent(r.getHotelId(), k -> new ArrayList<>()).add(r);
                }

                // 5 completed past bookings
                String[][] pastData = {
                                { "Rahul Sharma", "rahul.sharma@gmail.com", "+919876543210" },
                                { "Priya Nair", "priya.nair@gmail.com", "+919876543211" },
                                { "Amit Patel", "amit.patel@gmail.com", "+919876543212" },
                                { "Sneha Kapoor", "sneha.kapoor@gmail.com", "+919876543213" },
                                { "Vikram Singh", "vikram.singh@gmail.com", "+919876543214" },
                };

                for (int i = 0; i < 5; i++) {
                        User u = users.get(i + 1); // skip admin
                        Hotel h = hotels.get(i * 2 % hotels.size());
                        List<Room> hr = hotelRooms.getOrDefault(h.getId(), List.of());
                        if (hr.isEmpty())
                                continue;
                        Room r = hr.get(rng.nextInt(hr.size()));

                        int nights = 1 + rng.nextInt(4);
                        double room = r.getPricePerNight() * nights;
                        double tax = room * 0.12;
                        double disc = i == 0 ? 200 : (i == 1 ? 150 : 0);
                        double total = room + tax - disc;

                        LocalDate checkIn = LocalDate.now().minusDays(30 + i * 10);
                        LocalDate checkOut = checkIn.plusDays(nights);

                        bookings.add(Booking.builder()
                                        .userId(u.getId())
                                        .hotelId(h.getId())
                                        .roomId(r.getId())
                                        .checkIn(checkIn)
                                        .checkOut(checkOut)
                                        .guests(nights > 2 ? 2 : 1)
                                        .roomPriceTotal(room)
                                        .taxAmount(tax)
                                        .discountAmount(disc)
                                        .totalAmount(total)
                                        .guestName(pastData[i][0])
                                        .guestEmail(pastData[i][1])
                                        .guestPhone(pastData[i][2])
                                        .couponCode(disc > 0 ? "SAVE" + (int) disc : null)
                                        .status(BookingStatus.COMPLETED)
                                        .paymentStatus(PaymentStatus.PAID)
                                        .paymentId("pay_seed_" + (i + 1000))
                                        .razorpayOrderId("order_seed_" + (i + 2000))
                                        .build());
                }

                // 3 upcoming confirmed bookings
                for (int i = 0; i < 3; i++) {
                        User u = users.get((i % 5) + 1);
                        Hotel h = hotels.get(i * 3 % hotels.size());
                        List<Room> hr = hotelRooms.getOrDefault(h.getId(), List.of());
                        if (hr.isEmpty())
                                continue;
                        Room r = hr.get(rng.nextInt(hr.size()));

                        int nights = 2 + rng.nextInt(3);
                        double room = r.getPricePerNight() * nights;
                        double tax = room * 0.12;
                        double total = room + tax;

                        LocalDate checkIn = LocalDate.now().plusDays(5 + i * 7);
                        LocalDate checkOut = checkIn.plusDays(nights);

                        bookings.add(Booking.builder()
                                        .userId(u.getId())
                                        .hotelId(h.getId())
                                        .roomId(r.getId())
                                        .checkIn(checkIn)
                                        .checkOut(checkOut)
                                        .guests(2)
                                        .roomPriceTotal(room)
                                        .taxAmount(tax)
                                        .discountAmount(0.0)
                                        .totalAmount(total)
                                        .guestName(u.getName())
                                        .guestEmail(u.getEmail())
                                        .guestPhone(u.getPhone())
                                        .status(BookingStatus.CONFIRMED)
                                        .paymentStatus(PaymentStatus.PAID)
                                        .paymentId("pay_seed_upcom_" + i)
                                        .razorpayOrderId("order_seed_upcom_" + i)
                                        .build());
                }

                // 2 cancelled bookings
                for (int i = 0; i < 2; i++) {
                        User u = users.get((i + 2) % 5 + 1);
                        Hotel h = hotels.get(i * 4 % hotels.size());
                        List<Room> hr = hotelRooms.getOrDefault(h.getId(), List.of());
                        if (hr.isEmpty())
                                continue;
                        Room r = hr.get(0);

                        LocalDate checkIn = LocalDate.now().minusDays(60 + i * 15);
                        LocalDate checkOut = checkIn.plusDays(2);
                        double amount = r.getPricePerNight() * 2;

                        bookings.add(Booking.builder()
                                        .userId(u.getId())
                                        .hotelId(h.getId())
                                        .roomId(r.getId())
                                        .checkIn(checkIn)
                                        .checkOut(checkOut)
                                        .guests(1)
                                        .roomPriceTotal(amount)
                                        .taxAmount(amount * 0.12)
                                        .discountAmount(0.0)
                                        .totalAmount(amount * 1.12)
                                        .guestName(u.getName())
                                        .guestEmail(u.getEmail())
                                        .guestPhone(u.getPhone())
                                        .status(BookingStatus.CANCELLED)
                                        .paymentStatus(PaymentStatus.REFUNDED)
                                        .build());
                }

                bookingRepository.saveAll(bookings);
        }

        // ─────────────────────────── REVIEWS ────────────────────────────────────

        private void seedReviews(List<User> users, List<Hotel> hotels) {
                List<Review> reviews = new ArrayList<>();

                String[][] positiveComments = {
                                { "Absolutely loved the stay! The room was spotless and the staff was incredibly helpful. Will definitely come back." },
                                { "Great location with easy access to all the major attractions. Breakfast was delicious and the bed was super comfortable." },
                                { "Excellent value for money. Clean, modern rooms with fast WiFi. Perfect for a business trip." },
                                { "The rooftop view was stunning. Room service was prompt and the ambience was very relaxing." },
                                { "Perfect hotel for families. Kids loved the pool and the staff went out of their way to make us feel at home." },
                };

                String[][] avgComments = {
                                { "Decent stay overall. The room was clean but could use some renovation. Good location though." },
                                { "Average experience. WiFi was a bit slow but the check-in was easy and the staff was friendly." },
                                { "Okay for the price. Nothing extraordinary but comfortable enough for a short stay." },
                };

                Random rng = new Random(42);

                for (int hi = 0; hi < hotels.size(); hi++) {
                        Hotel h = hotels.get(hi);
                        int numReviews = 2 + rng.nextInt(2); // 2 or 3 reviews per hotel

                        for (int ri = 0; ri < numReviews; ri++) {
                                User u = users.get((hi + ri + 1) % (users.size() - 1) + 1);
                                boolean positive = rng.nextDouble() > 0.25;
                                int rating = positive ? (4 + rng.nextInt(2)) : (3 + rng.nextInt(2));
                                String comment = positive
                                                ? positiveComments[rng.nextInt(positiveComments.length)][0]
                                                : avgComments[rng.nextInt(avgComments.length)][0];

                                reviews.add(Review.builder()
                                                .userId(u.getId())
                                                .hotelId(h.getId())
                                                .rating(rating)
                                                .comment(comment)
                                                .build());
                        }
                }

                reviewRepository.saveAll(reviews);
        }

        // ─────────────────────────── COUPONS ────────────────────────────────────

        private void seedCoupons() {
                LocalDateTime twoMonthsLater = LocalDateTime.now().plusMonths(2);
                LocalDateTime oneMonthLater = LocalDateTime.now().plusMonths(1);

                List<Coupon> coupons = List.of(
                                Coupon.builder()
                                                .code("OYOFIRST")
                                                .title("First Booking Discount")
                                                .description("Get ₹300 off on your first OYO booking")
                                                .terms("Valid for new users only. Min booking ₹1500.")
                                                .discountType(DiscountType.FLAT)
                                                .discountValue(300.0)
                                                .minBookingAmount(1500.0)
                                                .maxDiscount(300.0)
                                                .expiryDate(twoMonthsLater)
                                                .usageLimit(1000)
                                                .isActive(true)
                                                .build(),

                                Coupon.builder()
                                                .code("SAVE200")
                                                .title("Flat ₹200 Off")
                                                .description("₹200 off on stays above ₹1000")
                                                .terms("One-time use. Not applicable on already discounted rates.")
                                                .discountType(DiscountType.FLAT)
                                                .discountValue(200.0)
                                                .minBookingAmount(1000.0)
                                                .maxDiscount(200.0)
                                                .expiryDate(twoMonthsLater)
                                                .usageLimit(5000)
                                                .isActive(true)
                                                .build(),

                                Coupon.builder()
                                                .code("OYOWEEKEND")
                                                .title("Weekend Getaway Deal")
                                                .description("15% off on weekend bookings")
                                                .terms("Valid Fri–Sun check-ins only. Min ₹2000.")
                                                .discountType(DiscountType.PERCENT)
                                                .discountValue(15.0)
                                                .minBookingAmount(2000.0)
                                                .maxDiscount(500.0)
                                                .expiryDate(twoMonthsLater)
                                                .usageLimit(2000)
                                                .isActive(true)
                                                .build(),

                                Coupon.builder()
                                                .code("OYOSUMMER")
                                                .title("Summer Sale — 20% Off")
                                                .description("Cool stays, hot deals! 20% off on any booking")
                                                .terms("Valid for stays ≥ 2 nights. Max discount ₹800.")
                                                .discountType(DiscountType.PERCENT)
                                                .discountValue(20.0)
                                                .minBookingAmount(2500.0)
                                                .maxDiscount(800.0)
                                                .expiryDate(oneMonthLater)
                                                .usageLimit(3000)
                                                .isActive(true)
                                                .build(),

                                Coupon.builder()
                                                .code("OYOBIZ")
                                                .title("Business Travel Coupon")
                                                .description("₹500 off for corporates. Relax after work.")
                                                .terms("Valid Mon–Thu. Requires company email at checkout.")
                                                .discountType(DiscountType.FLAT)
                                                .discountValue(500.0)
                                                .minBookingAmount(3000.0)
                                                .maxDiscount(500.0)
                                                .expiryDate(twoMonthsLater)
                                                .usageLimit(1500)
                                                .isActive(true)
                                                .build(),

                                Coupon.builder()
                                                .code("LOYALOYO")
                                                .title("Loyal Member — 25% Off")
                                                .description("Exclusive 25% off for OYO Loyalty members")
                                                .terms("Applicable once per month per user.")
                                                .discountType(DiscountType.PERCENT)
                                                .discountValue(25.0)
                                                .minBookingAmount(1200.0)
                                                .maxDiscount(1000.0)
                                                .expiryDate(twoMonthsLater)
                                                .usageLimit(500)
                                                .isActive(true)
                                                .build());

                couponRepository.saveAll(coupons);
        }

        // ─────────────────────────── HELPERS ────────────────────────────────────

        private Hotel hotel(String hostId, String name, String description,
                        String address, String city, String state, String pincode,
                        double lat, double lng, int stars,
                        List<String> amenities, List<String> images) {
                return Hotel.builder()
                                .hostId(hostId)
                                .name(name)
                                .description(description)
                                .address(address)
                                .city(city)
                                .state(state)
                                .country("India")
                                .pincode(pincode)
                                .latitude(lat)
                                .longitude(lng)
                                .starRating(stars)
                                .amenities(amenities)
                                .images(images)
                                .isApproved(true)
                                .isFeatured(stars >= 4)
                                .build();
        }

        private Room room(String hotelId, RoomType type, String number,
                        double price, double originalPrice, int maxOcc,
                        String description, String bedType, List<String> amenities) {
                return Room.builder()
                                .hotelId(hotelId)
                                .roomType(type)
                                .roomNumber(number)
                                .pricePerNight((double) Math.round(price))
                                .originalPrice((double) Math.round(originalPrice))
                                .maxOccupancy(maxOcc)
                                .description(description)
                                .bedType(bedType)
                                .amenities(amenities)
                                .images(roomImages(type))
                                .isAvailable(true)
                                .build();
        }

        private List<String> amenities(String... items) {
                return new ArrayList<>(List.of(items));
        }

        /**
         * Returns realistic Unsplash hotel image URLs for each city.
         * Using stable Unsplash Source URLs (no API key needed).
         */
        private List<String> hotelImages(String city, int variant) {
                // Curated hotel/city photo IDs from Unsplash
                Map<String, List<List<String>>> map = new LinkedHashMap<>();

                map.put("mumbai", List.of(
                                List.of(
                                                "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800",
                                                "https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800",
                                                "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=800",
                                                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800")));
                map.put("delhi", List.of(
                                List.of(
                                                "https://images.unsplash.com/photo-1601918774946-25832a4be0d6?w=800",
                                                "https://images.unsplash.com/photo-1590381105924-c72589b9ef3f?w=800",
                                                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=800",
                                                "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800",
                                                "https://images.unsplash.com/photo-1625244724120-1fd1d34d00f6?w=800")));
                map.put("bangalore", List.of(
                                List.of(
                                                "https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800",
                                                "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=800",
                                                "https://images.unsplash.com/photo-1455587734955-081b22074882?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800",
                                                "https://images.unsplash.com/photo-1549294413-26f195200c16?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1559599746-8823b38544c1?w=800",
                                                "https://images.unsplash.com/photo-1540541338287-41700207dee6?w=800")));
                map.put("goa", List.of(
                                List.of(
                                                "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800",
                                                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800",
                                                "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1584132915807-fd1f5fbc078f?w=800",
                                                "https://images.unsplash.com/photo-1610641818989-c2051b5e2cfd?w=800")));
                map.put("jaipur", List.of(
                                List.of(
                                                "https://images.unsplash.com/photo-1596436889106-be35e843f974?w=800",
                                                "https://images.unsplash.com/photo-1548013146-72479768bada?w=800",
                                                "https://images.unsplash.com/photo-1535930891776-0c2dfb7fda1a?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1573068815946-6f4c50be8f64?w=800",
                                                "https://images.unsplash.com/photo-1606046604972-77cc76aebb2d?w=800")));
                map.put("hyderabad", List.of(
                                List.of(
                                                "https://images.unsplash.com/photo-1551918120-9739cb430c6d?w=800",
                                                "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=800",
                                                "https://images.unsplash.com/photo-1586611292717-f828b167408c?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1602002418082-a4443e081dd1?w=800",
                                                "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=800")));
                map.put("chennai", List.of(
                                List.of(
                                                "https://images.unsplash.com/photo-1606046604972-77cc76aebb2d?w=800",
                                                "https://images.unsplash.com/photo-1595576508898-0ad5c879a061?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800",
                                                "https://images.unsplash.com/photo-1631049552057-403cdb8f0658?w=800")));
                map.put("pune", List.of(
                                List.of(
                                                "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800",
                                                "https://images.unsplash.com/photo-1614089858580-04d3f9e1b6eb?w=800",
                                                "https://images.unsplash.com/photo-1562790351-d273a961e0e9?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1556742111-a301076d9d18?w=800",
                                                "https://images.unsplash.com/photo-1504652517000-ae1068478c59?w=800"),
                                List.of(
                                                "https://images.unsplash.com/photo-1571508601891-ca5e7a713859?w=800",
                                                "https://images.unsplash.com/photo-1506059612708-99d6c258160e?w=800")));

                List<List<String>> cityVariants = map.getOrDefault(city, List.of(
                                List.of("https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800")));
                return new ArrayList<>(cityVariants.get(Math.min(variant - 1, cityVariants.size() - 1)));
        }

        private List<String> roomImages(RoomType type) {
                return switch (type) {
                        case SINGLE -> new ArrayList<>(List.of(
                                        "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800",
                                        "https://images.unsplash.com/photo-1595576508898-0ad5c879a061?w=800"));
                        case DOUBLE -> new ArrayList<>(List.of(
                                        "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=800",
                                        "https://images.unsplash.com/photo-1556742111-a301076d9d18?w=800"));
                        case DELUXE -> new ArrayList<>(List.of(
                                        "https://images.unsplash.com/photo-1614089858580-04d3f9e1b6eb?w=800",
                                        "https://images.unsplash.com/photo-1590381105924-c72589b9ef3f?w=800",
                                        "https://images.unsplash.com/photo-1563911302283-d2bc129e7570?w=800"));
                        case SUITE -> new ArrayList<>(List.of(
                                        "https://images.unsplash.com/photo-1596436889106-be35e843f974?w=800",
                                        "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
                                        "https://images.unsplash.com/photo-1551918120-9739cb430c6d?w=800"));
                };
        }
}
