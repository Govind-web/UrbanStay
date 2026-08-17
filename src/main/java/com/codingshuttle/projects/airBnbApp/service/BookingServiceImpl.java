package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.BookingDto;
import com.codingshuttle.projects.airBnbApp.dto.BookingRequest;
import com.codingshuttle.projects.airBnbApp.dto.GuestDto;
import com.codingshuttle.projects.airBnbApp.entity.Bookings;
import com.codingshuttle.projects.airBnbApp.entity.Guest;
import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.entity.enums.BookingStatus;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.repository.BookingRepository;
import com.codingshuttle.projects.airBnbApp.repository.GuestRepository;
import com.codingshuttle.projects.airBnbApp.repository.HotelRepository;
import com.codingshuttle.projects.airBnbApp.repository.InventoryRepository;
import com.codingshuttle.projects.airBnbApp.repository.RoomRepository;
import com.codingshuttle.projects.airBnbApp.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final GuestRepository guestRepository;
    private final ModelMapper modelMapper;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;


    @Override
    @Transactional
    public BookingDto initialiseBooking(
            BookingRequest bookingRequest
    ) {

        log.info(
                "Initialising booking | hotelId={}, roomId={}, checkIn={}, checkOut={}, roomsCount={}",
                bookingRequest.getHotelId(),
                bookingRequest.getRoomId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount()
        );


        // =========================================================
        // 1. Validate booking dates
        // =========================================================

        validateBookingDates(bookingRequest);


        // =========================================================
        // 2. Get authenticated user
        // =========================================================

        User currentUser = getCurrentUser();

        log.info(
                "Booking requested by userId={}, email={}",
                currentUser.getId(),
                currentUser.getEmail()
        );


        // =========================================================
        // 3. Find hotel
        // =========================================================

        Hotel hotel = hotelRepository
                .findById(bookingRequest.getHotelId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Hotel not found with id: "
                                        + bookingRequest.getHotelId()
                        )
                );


        // =========================================================
        // 4. Find room
        // =========================================================

        Room room = roomRepository
                .findById(bookingRequest.getRoomId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Room not found with id: "
                                        + bookingRequest.getRoomId()
                        )
                );


        // =========================================================
        // 5. Verify room belongs to hotel
        // =========================================================

        if (!room.getHotel().getId().equals(hotel.getId())) {

            throw new IllegalStateException(
                    "Room " + room.getId()
                            + " does not belong to hotel "
                            + hotel.getId()
            );
        }


        // =========================================================
        // 6. Check existing booking overlap
        // =========================================================

        Collection<BookingStatus> activeStatuses =
                List.of(
                        BookingStatus.RESERVED,
                        BookingStatus.GUEST_ADDED
                );


        boolean roomAlreadyBooked =
                bookingRepository.existsOverlappingBooking(
                        room.getId(),
                        bookingRequest.getCheckInDate(),
                        bookingRequest.getCheckOutDate(),
                        activeStatuses
                );


        if (roomAlreadyBooked) {

            log.warn(
                    "Room already booked | roomId={}, requestedCheckIn={}, requestedCheckOut={}",
                    room.getId(),
                    bookingRequest.getCheckInDate(),
                    bookingRequest.getCheckOutDate()
            );

            throw new IllegalStateException(
                    "Room is already booked for the selected dates"
            );
        }


        // =========================================================
        // 7. Find and lock inventory
        // =========================================================

        List<Inventory> inventoryList =
                inventoryRepository.findAndLockAvailableInventory(
                        room.getId(),
                        bookingRequest.getCheckInDate(),
                        bookingRequest.getCheckOutDate(),
                        bookingRequest.getRoomsCount()
                );


        // =========================================================
        // 8. Validate inventory availability
        // =========================================================

        long numberOfDays =
                bookingRequest.getCheckOutDate().toEpochDay()
                        -
                        bookingRequest.getCheckInDate().toEpochDay();


        if (inventoryList.size() < numberOfDays) {

            throw new IllegalStateException(
                    "Room is not available for the selected dates"
            );
        }


        // =========================================================
        // 9. Reserve inventory
        // =========================================================

        for (Inventory inventory : inventoryList) {

            inventory.setReservedCount(
                    inventory.getReservedCount()
                            + bookingRequest.getRoomsCount()
            );
        }

        inventoryRepository.saveAll(inventoryList);


        // =========================================================
        // 10. Calculate amount
        // =========================================================

        // TODO:
        // Replace this with your dynamic pricing calculation.

        BigDecimal amount = BigDecimal.TEN;


        // =========================================================
        // 11. Create booking
        // =========================================================

        Bookings booking = Bookings.builder()
                .hotel(hotel)
                .room(room)
                .user(currentUser)
                .bookingStatus(BookingStatus.RESERVED)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(amount)
                .build();


        // =========================================================
        // 12. Save booking
        // =========================================================

        booking = bookingRepository.save(booking);


        log.info(
                "Booking created successfully | bookingId={}, userId={}, hotelId={}, roomId={}, checkIn={}, checkOut={}",
                booking.getId(),
                currentUser.getId(),
                hotel.getId(),
                room.getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );


        // =========================================================
        // 13. Convert Entity -> DTO
        // =========================================================

        return modelMapper.map(
                booking,
                BookingDto.class
        );
    }


    @Override
    @Transactional
    public BookingDto addGuests(
            Long bookingId,
            List<GuestDto> guestDtoList
    ) {

        log.info(
                "Adding guests for bookingId={}",
                bookingId
        );


        // =========================================================
        // 1. Get authenticated user
        // =========================================================

        User currentUser = getCurrentUser();


        // =========================================================
        // 2. Find booking
        // =========================================================

        Bookings booking =
                bookingRepository.findById(bookingId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found with id: "
                                                + bookingId
                                )
                        );


        // =========================================================
        // 3. Check booking ownership
        // =========================================================

        if (!booking.getUser().getId()
                .equals(currentUser.getId())) {

            throw new IllegalStateException(
                    "You are not authorized to modify this booking"
            );
        }


        // =========================================================
        // 4. Check booking expiration
        // =========================================================

        if (isBookingExpired(booking)) {

            throw new IllegalStateException(
                    "Booking has already expired"
            );
        }


        // =========================================================
        // 5. Check booking status
        // =========================================================

        if (booking.getBookingStatus()
                != BookingStatus.RESERVED) {

            throw new IllegalStateException(
                    "Booking is not in RESERVED state, "
                            + "cannot add guests"
            );
        }


        // =========================================================
        // 6. Add guests
        // =========================================================

        for (GuestDto guestDto : guestDtoList) {

            Guest guest =
                    modelMapper.map(
                            guestDto,
                            Guest.class
                    );

            guest.setUser(currentUser);

            guest = guestRepository.save(guest);

            booking.getGuests().add(guest);
        }


        // =========================================================
        // 7. Update booking status
        // =========================================================

        booking.setBookingStatus(
                BookingStatus.GUEST_ADDED
        );


        // =========================================================
        // 8. Save booking
        // =========================================================

        booking = bookingRepository.save(booking);


        log.info(
                "Guests added successfully | bookingId={}",
                bookingId
        );


        // =========================================================
        // 9. Return DTO
        // =========================================================

        return modelMapper.map(
                booking,
                BookingDto.class
        );
    }


    // =============================================================
    // Validate booking dates
    // =============================================================

    private void validateBookingDates(
            BookingRequest bookingRequest
    ) {

        if (bookingRequest.getCheckInDate() == null
                || bookingRequest.getCheckOutDate() == null) {

            throw new IllegalArgumentException(
                    "Check-in and check-out dates are required"
            );
        }


        if (!bookingRequest.getCheckInDate()
                .isBefore(
                        bookingRequest.getCheckOutDate()
                )) {

            throw new IllegalArgumentException(
                    "Check-in date must be before check-out date"
            );
        }


        if (bookingRequest.getRoomsCount() == null
                || bookingRequest.getRoomsCount() <= 0) {

            throw new IllegalArgumentException(
                    "Rooms count must be greater than zero"
            );
        }
    }


    // =============================================================
    // Check booking expiration
    // =============================================================

    private boolean isBookingExpired(
            Bookings booking
    ) {

        return booking.getCreatedAt()
                .plusMinutes(10)
                .isBefore(LocalDateTime.now());
    }


    // =============================================================
    // Get current authenticated user
    // =============================================================

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }


        String username =
                authentication.getName();


        log.info(
                "Fetching authenticated user: {}",
                username
        );


        return userRepository
                .findByEmail(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with email: "
                                        + username
                        )
                );
    }

}