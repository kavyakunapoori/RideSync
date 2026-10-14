package com.carpool.ridesharing.repository;

import com.carpool.ridesharing.entity.Booking;
import com.carpool.ridesharing.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByPassengerIdOrderByBookingTimeDesc(Long passengerId);

    List<Booking> findByRideDriverIdOrderByBookingTimeDesc(Long driverId);

    List<Booking> findByRideId(Long rideId);

    List<Booking> findByRideIdAndStatus(Long rideId, BookingStatus status);

    List<Booking> findAllByOrderByBookingTimeDesc();

    boolean existsByRideIdAndPassengerIdAndStatusIn(Long rideId, Long passengerId, List<BookingStatus> statuses);

    long countByStatus(BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'CONFIRMED' OR b.status = 'COMPLETED'")
    long countCompletedOrConfirmedBookings();
}
