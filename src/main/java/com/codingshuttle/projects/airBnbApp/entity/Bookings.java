package com.codingshuttle.projects.airBnbApp.entity;

import com.codingshuttle.projects.airBnbApp.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Bookings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // Hotel
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;


    // =========================================================
    // Room
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;


    // =========================================================
    // User
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    // =========================================================
    // Number of rooms
    // =========================================================

    @Column(nullable = false)
    private Integer roomsCount;


    // =========================================================
    // Check-in date
    // =========================================================

    @Column(nullable = false)
    private LocalDate checkInDate;


    // =========================================================
    // Check-out date
    // =========================================================

    @Column(nullable = false)
    private LocalDate checkOutDate;


    // =========================================================
    // Created / Updated timestamps
    // =========================================================

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    // =========================================================
    // Booking Status
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus bookingStatus;


    // =========================================================
    // Guests
    // =========================================================

    @ManyToMany
    @JoinTable(
            name = "booking_guest",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "guest_id")
    )
    private Set<Guest> guests;


    // =========================================================
    // Amount
    // =========================================================

    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal amount;
}