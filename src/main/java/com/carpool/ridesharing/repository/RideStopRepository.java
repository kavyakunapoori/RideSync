package com.carpool.ridesharing.repository;

import com.carpool.ridesharing.entity.RideStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideStopRepository extends JpaRepository<RideStop, Long> {
    List<RideStop> findByRideIdOrderByStopOrderAsc(Long rideId);
}
