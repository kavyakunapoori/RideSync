package com.carpool.ridesharing.service;

import com.carpool.ridesharing.dto.RideCreateDto;
import com.carpool.ridesharing.dto.RideStopDto;
import com.carpool.ridesharing.entity.*;
import com.carpool.ridesharing.exception.InvalidOperationException;
import com.carpool.ridesharing.exception.ResourceNotFoundException;
import com.carpool.ridesharing.repository.BookingRepository;
import com.carpool.ridesharing.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final VehicleService vehicleService;
    private final FareSharingService fareSharingService;
    private final SmartRideMatchingService matchingService;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public RideService(RideRepository rideRepository,
                       VehicleService vehicleService,
                       FareSharingService fareSharingService,
                       SmartRideMatchingService matchingService,
                       BookingRepository bookingRepository,
                       NotificationService notificationService) {
        this.rideRepository = rideRepository;
        this.vehicleService = vehicleService;
        this.fareSharingService = fareSharingService;
        this.matchingService = matchingService;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Ride publishRide(User driver, RideCreateDto dto) {
        Vehicle vehicle = vehicleService.getVehicleByIdAndDriver(dto.getVehicleId(), driver.getId());

        if (dto.getTotalSeats() > vehicle.getCapacity()) {
            throw new InvalidOperationException("Offered seats (" + dto.getTotalSeats() +
                    ") exceed vehicle capacity (" + vehicle.getCapacity() + ").");
        }

        // Resolve coordinates for origin and destination if not supplied
        double[] originCoords = matchingService.resolveCoordinates(dto.getSourceAddress(), dto.getSourceLatitude(), dto.getSourceLongitude());
        double[] destCoords = matchingService.resolveCoordinates(dto.getDestinationAddress(), dto.getDestinationLatitude(), dto.getDestinationLongitude());

        // Calculate passenger per-seat cost
        BigDecimal passengerCost = fareSharingService.calculatePassengerCost(
                dto.getTotalTripCost(),
                dto.getDriverContributionPercent(),
                dto.getTotalSeats()
        );

        Ride ride = new Ride(
                driver,
                vehicle,
                dto.getSourceAddress().trim(),
                originCoords[0],
                originCoords[1],
                dto.getDestinationAddress().trim(),
                destCoords[0],
                destCoords[1],
                dto.getDepartureDate(),
                dto.getDepartureTime(),
                dto.getTotalSeats(),
                dto.getTotalTripCost(),
                dto.getDriverContributionPercent(),
                passengerCost
        );
        ride.setNotes(dto.getNotes());

        // Add intermediate stops if provided
        if (dto.getStops() != null && !dto.getStops().isEmpty()) {
            int order = 1;
            for (RideStopDto stopDto : dto.getStops()) {
                if (stopDto.getStopName() != null && !stopDto.getStopName().trim().isEmpty()) {
                    double[] stopCoords = matchingService.resolveCoordinates(stopDto.getStopName(), stopDto.getLatitude(), stopDto.getLongitude());
                    RideStop stop = new RideStop(ride, stopDto.getStopName().trim(), stopCoords[0], stopCoords[1], order++);
                    ride.addStop(stop);
                }
            }
        }

        return rideRepository.save(ride);
    }

    public Ride getRideById(Long id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));
    }

    public List<Ride> getRidesByDriver(Long driverId) {
        return rideRepository.findByDriverIdOrderByDepartureDateDescDepartureTimeDesc(driverId);
    }

    public List<Ride> getAllRides() {
        return rideRepository.findAllByOrderByDepartureDateDescDepartureTimeDesc();
    }

    @Transactional
    public void cancelRide(Long rideId, Long driverId, String reason) {
        Ride ride = getRideById(rideId);
        if (!ride.getDriver().getId().equals(driverId)) {
            throw new InvalidOperationException("You are not authorized to cancel this ride.");
        }
        if (ride.getStatus() == RideStatus.CANCELLED || ride.getStatus() == RideStatus.COMPLETED) {
            throw new InvalidOperationException("Cannot cancel a ride that is already " + ride.getStatus());
        }

        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);

        // Cancel all bookings and notify passengers
        List<Booking> bookings = bookingRepository.findByRideId(rideId);
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancellationReason("Driver cancelled trip: " + (reason != null ? reason : "No reason provided"));
                bookingRepository.save(booking);

                notificationService.sendNotification(
                        booking.getPassenger(),
                        "Ride Cancelled by Driver",
                        "The ride from " + ride.getSourceAddress() + " to " + ride.getDestinationAddress() +
                                " on " + ride.getDepartureDate() + " was cancelled. Reason: " +
                                (reason != null ? reason : "No reason specified"),
                        NotificationType.RIDE_CANCELLED
                );
            }
        }
    }

    @Transactional
    public void completeRide(Long rideId, Long driverId) {
        Ride ride = getRideById(rideId);
        if (!ride.getDriver().getId().equals(driverId)) {
            throw new InvalidOperationException("You are not authorized to complete this ride.");
        }
        if (ride.getStatus() != RideStatus.PLANNED && ride.getStatus() != RideStatus.ACTIVE) {
            throw new InvalidOperationException("Only planned or active rides can be marked as completed.");
        }

        ride.setStatus(RideStatus.COMPLETED);
        rideRepository.save(ride);

        // Mark active bookings as completed and notify passengers for review
        List<Booking> bookings = bookingRepository.findByRideId(rideId);
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);

                notificationService.sendNotification(
                        booking.getPassenger(),
                        "Trip Completed - Rate Your Driver",
                        "Your trip to " + ride.getDestinationAddress() + " has completed. Please leave a rating and review for " +
                                ride.getDriver().getFullName() + "!",
                        NotificationType.RIDE_COMPLETED
                );
            }
        }
    }
}
