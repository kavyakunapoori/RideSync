package com.carpool.ridesharing.repository;

import com.carpool.ridesharing.entity.Ride;
import com.carpool.ridesharing.entity.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByDriverIdOrderByDepartureDateDescDepartureTimeDesc(Long driverId);

    List<Ride> findByStatusAndDepartureDateGreaterThanEqual(RideStatus status, LocalDate date);

    List<Ride> findByStatusAndAvailableSeatsGreaterThanAndDepartureDateGreaterThanEqual(
            RideStatus status, int minSeats, LocalDate date);

    List<Ride> findAllByOrderByDepartureDateDescDepartureTimeDesc();

    long countByStatus(RideStatus status);

    @Query("SELECT r FROM Ride r WHERE r.status = :status AND r.availableSeats > 0 " +
           "AND r.departureDate >= :minDate ORDER BY r.departureDate ASC, r.departureTime ASC")
    List<Ride> findActiveAvailableRides(@Param("status") RideStatus status, @Param("minDate") LocalDate minDate);
}
