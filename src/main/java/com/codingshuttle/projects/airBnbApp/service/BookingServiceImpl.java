package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.BookingDto;
import com.codingshuttle.projects.airBnbApp.dto.BookingRequest;
import com.codingshuttle.projects.airBnbApp.dto.GuestDto;
import com.codingshuttle.projects.airBnbApp.entity.*;
import com.codingshuttle.projects.airBnbApp.entity.enums.BookingStatus;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.repository.*;
import jakarta.transaction.Transactional;
import jdk.jfr.TransitionTo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.internal.bytebuddy.asm.Advice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements  BookingService{
    private final GuestRepository guestRepository;
    private final ModelMapper modelMapper;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public BookingDto initialiseBooking(BookingRequest bookingRequest) {
        log.info("initialising booking for hotel : {},room : {},date {}-{}",bookingRequest.getHotelId(),
                bookingRequest.getRoomId(),bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate());
        Hotel hotel=hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(()->
                new ResourceNotFoundException("Hotel not found with id : "+bookingRequest.getHotelId()));
        Room room=roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(()->
                        new ResourceNotFoundException("Room not found with id : "+bookingRequest.getRoomId()));
        List<Inventory> inventoryList=inventoryRepository.findAndLockAvailableInventory(room.getId(),
                bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate(),bookingRequest.getRoomsCount());
        long daysCount= ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate())+1;
        if(inventoryList.size() !=daysCount){
            throw new IllegalStateException("Room is not available anymore");
        }
        //Reserve the room / update the booked count of inventories
        for(Inventory inventory:inventoryList){
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }
        inventoryRepository.saveAll(inventoryList);


        //TODO: calculate dynamic amount
        Bookings bookings=Bookings.builder()
                .hotel(hotel)
                .bookingStatus(BookingStatus.RESERVED)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.TEN)
                .build();
        bookings=bookingRepository.save(bookings);

        return modelMapper.map(bookings,BookingDto.class);
    }

    @Override
    @Transactional
    public BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList) {
        log.info("Adding guests for booking with id: {}",bookingId);
        Bookings bookings=bookingRepository.findById(bookingId).orElseThrow(()->
                new ResourceNotFoundException("Booking not found with id: "+bookingId));
        if(isBookingExpired(bookings)){
            throw new IllegalStateException("Booking has already expired");
        }
        if(bookings.getBookingStatus() !=BookingStatus.RESERVED){
            throw new IllegalStateException("Booking not under reserved state,cannot add guests");
        }
        for(GuestDto guestDto:guestDtoList){
            Guest guest=modelMapper.map(guestDto,Guest.class);
            guest.setUser(getCurrentUser());
            guest= guestRepository.save(guest);
            bookings.getGuests().add(guest);
        }
        bookings.setBookingStatus(BookingStatus.GUEST_ADDED);
        bookings=bookingRepository.save(bookings);
        return modelMapper.map(bookings,BookingDto.class);
    }
    public boolean isBookingExpired(Bookings bookings){
        return bookings.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }
    public User getCurrentUser(){
        //create the booking
        User user=new User();
        user.setId(1L);  //TODO REMOVE Dummy user
        return user;
    }
}
