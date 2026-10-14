package com.carpool.ridesharing.service;

import com.carpool.ridesharing.dto.BookingRequestDto;
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
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository,
                          RideRepository rideRepository,
                          NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.notificationService = notificationService;
    }

    /**
     * Concurrency-safe ride reservation.
     * Checks seat availability and reduces seats within a transactional boundary.
     */
    @Transactional
    public Booking createBooking(User passenger, BookingRequestDto dto) {
        Ride ride = rideRepository.findById(dto.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + dto.getRideId()));

        if (ride.getStatus() != RideStatus.PLANNED) {
            throw new InvalidOperationException("This ride is no longer available for booking (Status: " + ride.getStatus() + ").");
        }

        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new InvalidOperationException("Drivers cannot book seats on their own published rides.");
        }

        int requestedSeats = dto.getSeatsBooked() != null ? dto.getSeatsBooked() : 1;

        if (ride.getAvailableSeats() < requestedSeats) {
            throw new InvalidOperationException("Not enough seats available. Requested: " +
                    requestedSeats + ", Available: " + ride.getAvailableSeats());
        }

        // Prevent duplicate active booking by same passenger
        boolean alreadyBooked = bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(
                ride.getId(), passenger.getId(), List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING));
        if (alreadyBooked) {
            throw new InvalidOperationException("You already have an active booking for this ride.");
        }

        // Atomically reserve seats
        ride.setAvailableSeats(ride.getAvailableSeats() - requestedSeats);
        rideRepository.save(ride);

        BigDecimal totalFare = ride.getPassengerCost().multiply(BigDecimal.valueOf(requestedSeats));

        // Create booking with CONFIRMED status (or PENDING if approval required)
        Booking booking = new Booking(
                ride,
                passenger,
                requestedSeats,
                totalFare,
                dto.getPickupLocation() != null ? dto.getPickupLocation() : ride.getSourceAddress(),
                dto.getDropoffLocation() != null ? dto.getDropoffLocation() : ride.getDestinationAddress(),
                BookingStatus.CONFIRMED
        );

        Booking savedBooking = bookingRepository.save(booking);

        // Notify Driver
        notificationService.sendNotification(
                ride.getDriver(),
                "New Passenger Booking Confirmed",
                passenger.getFullName() + " booked " + requestedSeats + " seat(s) on your trip from " +
                        ride.getSourceAddress() + " to " + ride.getDestinationAddress() + " (Total fare: $" + totalFare + ").",
                NotificationType.BOOKING_REQUEST
        );

        // Notify Passenger
        notificationService.sendNotification(
                passenger,
                "Booking Confirmed!",
                "Your reservation for " + requestedSeats + " seat(s) on the ride to " +
                        ride.getDestinationAddress() + " has been confirmed.",
                NotificationType.BOOKING_ACCEPTED
        );

        return savedBooking;
    }

    @Transactional
    public void acceptBooking(Long bookingId, Long driverId) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getRide().getDriver().getId().equals(driverId)) {
            throw new InvalidOperationException("Only the ride driver can approve this booking.");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidOperationException("Booking is not in pending status.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        notificationService.sendNotification(
                booking.getPassenger(),
                "Booking Approved!",
                "Your booking request has been accepted by the driver.",
                NotificationType.BOOKING_ACCEPTED
        );
    }

    @Transactional
    public void rejectBooking(Long bookingId, Long driverId, String reason) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getRide().getDriver().getId().equals(driverId)) {
            throw new InvalidOperationException("Only the ride driver can reject this booking.");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidOperationException("Only pending bookings can be rejected.");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setCancellationReason(reason != null ? reason : "Declined by driver");
        bookingRepository.save(booking);

        // Restore seats
        Ride ride = booking.getRide();
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        rideRepository.save(ride);

        notificationService.sendNotification(
                booking.getPassenger(),
                "Booking Declined",
                "Your booking request was declined by the driver. Reason: " +
                        (reason != null ? reason : "No reason specified"),
                NotificationType.BOOKING_REJECTED
        );
    }

    @Transactional
    public void cancelBooking(Long bookingId, Long userId, String reason) {
        Booking booking = getBookingById(bookingId);
        boolean isPassenger = booking.getPassenger().getId().equals(userId);
        boolean isDriver = booking.getRide().getDriver().getId().equals(userId);

        if (!isPassenger && !isDriver) {
            throw new InvalidOperationException("You are not authorized to cancel this booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new InvalidOperationException("Booking cannot be cancelled because it is " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason != null ? reason : (isPassenger ? "Cancelled by passenger" : "Cancelled by driver"));
        bookingRepository.save(booking);

        // Return reserved seats back to ride
        Ride ride = booking.getRide();
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        rideRepository.save(ride);

        // Notify the counterparty
        if (isPassenger) {
            notificationService.sendNotification(
                    ride.getDriver(),
                    "Passenger Cancelled Booking",
                    booking.getPassenger().getFullName() + " cancelled their booking of " +
                            booking.getSeatsBooked() + " seat(s) for the trip to " + ride.getDestinationAddress() + ".",
                    NotificationType.BOOKING_CANCELLED
            );
        } else {
            notificationService.sendNotification(
                    booking.getPassenger(),
                    "Booking Cancelled by Driver",
                    "The driver cancelled your booking for the trip to " + ride.getDestinationAddress() + ".",
                    NotificationType.BOOKING_CANCELLED
            );
        }
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    public List<Booking> getPassengerBookings(Long passengerId) {
        return bookingRepository.findByPassengerIdOrderByBookingTimeDesc(passengerId);
    }

    public List<Booking> getDriverBookings(Long driverId) {
        return bookingRepository.findByRideDriverIdOrderByBookingTimeDesc(driverId);
    }

    public List<Booking> getBookingsForRide(Long rideId) {
        return bookingRepository.findByRideId(rideId);
    }
}
