package com.oyo.backend.service;

import com.oyo.backend.dto.booking.BookingRequest;
import com.oyo.backend.dto.booking.BookingResponse;
import com.oyo.backend.entity.Booking;
import com.oyo.backend.entity.Hotel;
import com.oyo.backend.entity.Room;
import com.oyo.backend.entity.User;
import com.oyo.backend.enums.BookingStatus;
import com.oyo.backend.enums.DiscountType;
import com.oyo.backend.enums.NotificationType;
import com.oyo.backend.enums.PaymentStatus;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.BookingRepository;
import com.oyo.backend.repository.CouponRepository;
import com.oyo.backend.repository.HotelRepository;
import com.oyo.backend.repository.RoomRepository;
import com.oyo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    private static final double TAX_RATE = 0.18; // 18% GST

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String userId) {
        // Validate dates
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new ApiException("Check-out must be after check-in");
        }

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ApiException("Hotel not found", HttpStatus.NOT_FOUND));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        // Check room availability
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                request.getRoomId(), request.getCheckIn(), request.getCheckOut());
        if (!conflicts.isEmpty()) {
            throw new ApiException("Room is not available for the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        double roomPriceTotal = room.getPricePerNight() * nights;

        // Apply coupon
        double discountAmount = 0.0;
        String appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            var couponOpt = couponRepository.findByCodeIgnoreCase(request.getCouponCode());
            if (couponOpt.isPresent()) {
                var coupon = couponOpt.get();
                if (coupon.getIsActive()
                        && coupon.getExpiryDate().isAfter(LocalDateTime.now())
                        && (coupon.getUsageLimit() == null || coupon.getUsedCount() < coupon.getUsageLimit())
                        && (coupon.getMinBookingAmount() == null || roomPriceTotal >= coupon.getMinBookingAmount())) {
                    if (coupon.getDiscountType() == DiscountType.FLAT) {
                        discountAmount = coupon.getDiscountValue();
                    } else {
                        discountAmount = roomPriceTotal * (coupon.getDiscountValue() / 100);
                        if (coupon.getMaxDiscount() != null) {
                            discountAmount = Math.min(discountAmount, coupon.getMaxDiscount());
                        }
                    }
                    coupon.setUsedCount(coupon.getUsedCount() + 1);
                    couponRepository.save(coupon);
                    appliedCoupon = coupon.getCode();
                }
            }
        }

        double taxableAmount = roomPriceTotal - discountAmount;
        double taxAmount = taxableAmount * TAX_RATE;
        double totalAmount = taxableAmount + taxAmount;

        Booking booking = Booking.builder()
                .userId(userId)
                .hotelId(request.getHotelId())
                .roomId(request.getRoomId())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .guests(request.getGuests())
                .roomPriceTotal(roomPriceTotal)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .couponCode(appliedCoupon)
                .guestName(request.getGuestName() != null ? request.getGuestName() : user.getName())
                .guestEmail(request.getGuestEmail() != null ? request.getGuestEmail() : user.getEmail())
                .guestPhone(request.getGuestPhone() != null ? request.getGuestPhone() : user.getPhone())
                .specialRequests(request.getSpecialRequests())
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        booking = bookingRepository.save(booking);

        notificationService.createNotification(userId,
                "Booking Created",
                "Your booking at " + hotel.getName() + " has been created. Complete payment to confirm.",
                NotificationType.BOOKING, booking.getId());

        return toResponse(booking, hotel, room);
    }

    public Page<BookingResponse> getUserBookings(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(b -> {
                    Hotel hotel = hotelRepository.findById(b.getHotelId()).orElse(null);
                    Room room = roomRepository.findById(b.getRoomId()).orElse(null);
                    return toResponse(b, hotel, room);
                });
    }

    public BookingResponse getBookingById(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
        if (!booking.getUserId().equals(userId)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        Hotel hotel = hotelRepository.findById(booking.getHotelId()).orElse(null);
        Room room = roomRepository.findById(booking.getRoomId()).orElse(null);
        return toResponse(booking, hotel, room);
    }

    @Transactional
    public BookingResponse cancelBooking(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
        if (!booking.getUserId().equals(userId)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ApiException("Booking already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ApiException("Cannot cancel a completed booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        Hotel hotel = hotelRepository.findById(booking.getHotelId()).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (hotel != null && user != null) {
            emailService.sendBookingCancellationEmail(user.getEmail(), user.getName(),
                    booking.getId(), hotel.getName());
        }

        notificationService.createNotification(userId, "Booking Cancelled",
                "Your booking" + (hotel != null ? " at " + hotel.getName() : "") + " has been cancelled.",
                NotificationType.BOOKING, bookingId);

        Room room = roomRepository.findById(booking.getRoomId()).orElse(null);
        return toResponse(booking, hotel, room);
    }

    public Page<BookingResponse> getAllBookings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookingRepository.findAllByOrderByCreatedAtDesc(pageable).map(b -> {
            Hotel hotel = hotelRepository.findById(b.getHotelId()).orElse(null);
            Room room = roomRepository.findById(b.getRoomId()).orElse(null);
            return toResponse(b, hotel, room);
        });
    }

    @Transactional
    public BookingResponse updateBookingStatus(String bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
        booking.setStatus(status);
        booking = bookingRepository.save(booking);

        Hotel hotel = hotelRepository.findById(booking.getHotelId()).orElse(null);

        notificationService.createNotification(booking.getUserId(), "Booking Updated",
                "Your booking status has been updated to " + status.name(),
                NotificationType.BOOKING, bookingId);

        if (status == BookingStatus.CONFIRMED && hotel != null) {
            User user = userRepository.findById(booking.getUserId()).orElse(null);
            if (user != null) {
                emailService.sendBookingConfirmationEmail(user.getEmail(), user.getName(),
                        booking.getId(), hotel.getName(),
                        booking.getCheckIn().toString(), booking.getCheckOut().toString(),
                        booking.getTotalAmount());
            }
        }

        Room room = roomRepository.findById(booking.getRoomId()).orElse(null);
        return toResponse(booking, hotel, room);
    }

    @Scheduled(cron = "0 0 1 * * *") // Run daily at 1 AM
    @Transactional
    public void autoCompleteBookings() {
        List<Booking> confirmeds = bookingRepository.findByStatusAndCheckOutBefore(
                BookingStatus.CONFIRMED, LocalDate.now());
        confirmeds.forEach(b -> {
            b.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(b);
        });
    }

    private BookingResponse toResponse(Booking b, Hotel hotel, Room room) {
        long nights = b.getCheckIn() != null && b.getCheckOut() != null
                ? ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut())
                : 0;
        return BookingResponse.builder()
                .id(b.getId())
                .hotelId(b.getHotelId())
                .hotelName(hotel != null ? hotel.getName() : null)
                .hotelImage(hotel != null && !hotel.getImages().isEmpty() ? hotel.getImages().get(0) : null)
                .hotelCity(hotel != null ? hotel.getCity() : null)
                .roomId(b.getRoomId())
                .roomType(room != null ? room.getRoomType().name() : null)
                .roomNumber(room != null ? room.getRoomNumber() : null)
                .checkIn(b.getCheckIn())
                .checkOut(b.getCheckOut())
                .nights((int) nights)
                .guests(b.getGuests())
                .roomPriceTotal(b.getRoomPriceTotal())
                .taxAmount(b.getTaxAmount())
                .discountAmount(b.getDiscountAmount())
                .totalAmount(b.getTotalAmount())
                .couponCode(b.getCouponCode())
                .guestName(b.getGuestName())
                .guestEmail(b.getGuestEmail())
                .guestPhone(b.getGuestPhone())
                .specialRequests(b.getSpecialRequests())
                .status(b.getStatus())
                .paymentStatus(b.getPaymentStatus())
                .paymentId(b.getPaymentId())
                .razorpayOrderId(b.getRazorpayOrderId())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
