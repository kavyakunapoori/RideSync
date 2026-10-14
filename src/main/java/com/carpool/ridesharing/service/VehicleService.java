package com.carpool.ridesharing.service;

import com.carpool.ridesharing.dto.VehicleDto;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.entity.Vehicle;
import com.carpool.ridesharing.exception.InvalidOperationException;
import com.carpool.ridesharing.exception.ResourceNotFoundException;
import com.carpool.ridesharing.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public Vehicle addVehicle(User driver, VehicleDto dto) {
        if (vehicleRepository.existsByLicensePlate(dto.getLicensePlate().toUpperCase().trim())) {
            throw new InvalidOperationException("A vehicle with license plate " + dto.getLicensePlate() + " is already registered.");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setMake(dto.getMake().trim());
        vehicle.setModel(dto.getModel().trim());
        vehicle.setYear(dto.getYear());
        vehicle.setLicensePlate(dto.getLicensePlate().toUpperCase().trim());
        vehicle.setColor(dto.getColor() != null ? dto.getColor().trim() : "Unknown");
        vehicle.setCapacity(dto.getCapacity());
        vehicle.setActive(true);

        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getVehiclesByDriver(Long driverId) {
        return vehicleRepository.findByDriverIdAndActiveTrue(driverId);
    }

    public Vehicle getVehicleByIdAndDriver(Long vehicleId, Long driverId) {
        return vehicleRepository.findByIdAndDriverId(vehicleId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found or does not belong to driver."));
    }
}
