package com.carpool.ridesharing.repository;

import com.carpool.ridesharing.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByDriverIdAndActiveTrue(Long driverId);
    Optional<Vehicle> findByIdAndDriverId(Long id, Long driverId);
    boolean existsByLicensePlate(String licensePlate);
}
