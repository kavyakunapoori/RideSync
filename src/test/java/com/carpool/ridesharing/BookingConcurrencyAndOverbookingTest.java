package com.carpool.ridesharing;

import com.carpool.ridesharing.dto.BookingRequestDto;
import com.carpool.ridesharing.entity.*;
import com.carpool.ridesharing.exception.InvalidOperationException;
import com.carpool.ridesharing.repository.BookingRepository;
import com.carpool.ridesharing.repository.NotificationRepository;
import com.carpool.ridesharing.repository.RideRepository;
import com.carpool.ridesharing.service.BookingService;
import com.carpool.ridesharing.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BookingConcurrencyAndOverbookingTest {

    private BookingRepository bookingRepository;
    private RideRepository rideRepository;
    private NotificationRepository notificationRepository;
    private NotificationService notificationService;
    private BookingService bookingService;

    private User driver;
    private User passenger;
    private Ride ride;

    @BeforeEach
    void setUp() {
        bookingRepository = Mockito.mock(BookingRepository.class);
        rideRepository = Mockito.mock(RideRepository.class);
        notificationRepository = Mockito.mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository);
        bookingService = new BookingService(bookingRepository, rideRepository, notificationService);

        driver = new User("driver@carpool.com", "pass", "Driver John", "111");
        driver.setId(1L);

        passenger = new User("passenger@carpool.com", "pass", "Alice Passenger", "222");
        passenger.setId(2L);

        Vehicle vehicle = new Vehicle(driver, "Toyota", "Camry", 2023, "XYZ-123", "Silver", 4);

        ride = new Ride(
                driver, vehicle,
                "Origin", 40.71, -74.00,
                "Destination", 40.75, -73.98,
                LocalDate.now().plusDays(1), LocalTime.of(9, 0),
                3, BigDecimal.valueOf(18.00), 0.0, BigDecimal.valueOf(6.00)
        );
        ride.setId(100L);
        ride.setAvailableSeats(2); // Only 2 seats available
    }

    @Test
    @DisplayName("Should successfully book available seats and deduct from ride availableSeats")
    void testSuccessfulBookingReducesSeats() {
        when(rideRepository.findById(100L)).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingRequestDto dto = new BookingRequestDto(100L, 1);
        Booking result = bookingService.createBooking(passenger, dto);

        assertNotNull(result);
        assertEquals(1, result.getSeatsBooked());
        // Available seats was 2, now should be 1
        assertEquals(1, ride.getAvailableSeats());
        verify(rideRepository, times(1)).save(ride);
    }

    @Test
    @DisplayName("Should reject booking if requested seats exceed available seats (prevent overbooking)")
    void testOverbookingPrevention() {
        when(rideRepository.findById(100L)).thenReturn(Optional.of(ride));

        // Attempt to book 3 seats when only 2 are available
        BookingRequestDto dto = new BookingRequestDto(100L, 3);

        assertThrows(InvalidOperationException.class, () -> {
            bookingService.createBooking(passenger, dto);
        });

        // Seats must remain unchanged
        assertEquals(2, ride.getAvailableSeats());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Driver should be prohibited from booking seats on their own ride")
    void testDriverCannotBookOwnRide() {
        when(rideRepository.findById(100L)).thenReturn(Optional.of(ride));

        BookingRequestDto dto = new BookingRequestDto(100L, 1);

        assertThrows(InvalidOperationException.class, () -> {
            bookingService.createBooking(driver, dto); // Driver attempts to book
        });
    }

    @Test
    @DisplayName("Cancelling a booking should release and return seats back to the ride")
    void testCancellationRestoresSeats() {
        Booking booking = new Booking(ride, passenger, 2, BigDecimal.valueOf(12.00), "Origin", "Destination", BookingStatus.CONFIRMED);
        booking.setId(500L);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        // Current available seats = 2
        bookingService.cancelBooking(500L, passenger.getId(), "Change of plans");

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        // 2 originally available + 2 released seats = 4
        assertEquals(4, ride.getAvailableSeats());
        verify(rideRepository, times(1)).save(ride);
    }
}
