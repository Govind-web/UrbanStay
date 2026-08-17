package com.codingshuttle.projects.airBnbApp.repository;

import com.codingshuttle.projects.airBnbApp.entity.Bookings;
import com.codingshuttle.projects.airBnbApp.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

public interface BookingRepository extends JpaRepository<Bookings, Long> {

    @Query("""
        SELECT COUNT(b) > 0
        FROM Bookings b
        WHERE b.room.id = :roomId
        AND b.bookingStatus IN :activeStatuses
        AND b.checkInDate < :requestedCheckOut
        AND b.checkOutDate > :requestedCheckIn
        """)
    boolean existsOverlappingBooking(
            @Param("roomId") Long roomId,
            @Param("requestedCheckIn") LocalDate requestedCheckIn,
            @Param("requestedCheckOut") LocalDate requestedCheckOut,
            @Param("activeStatuses") Collection<BookingStatus> activeStatuses
    );
}