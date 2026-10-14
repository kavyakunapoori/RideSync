package com.carpool.ridesharing.config;

import com.carpool.ridesharing.entity.*;
import com.carpool.ridesharing.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seed data initializer for college demonstration and development.
 * Pre-populates administrative account, demo drivers, passengers, vehicles,
 * published rides, intermediate stops, completed bookings, ratings, and notifications.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final RatingRepository ratingRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           VehicleRepository vehicleRepository,
                           RideRepository rideRepository,
                           BookingRepository bookingRepository,
                           RatingRepository ratingRepository,
                           NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.ratingRepository = ratingRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        logger.info("Checking system roles and demo seed data...");

        // 1. Roles
        Role rolePassenger = getOrCreateRole("ROLE_PASSENGER");
        Role roleDriver = getOrCreateRole("ROLE_DRIVER");
        Role roleAdmin = getOrCreateRole("ROLE_ADMIN");

        // 2. Admin account
        if (userRepository.findByEmail("admin@carpool.com").isEmpty()) {
            User admin = new User("admin@carpool.com", passwordEncoder.encode("Admin123!"), "System Administrator", "+1 555-0100");
            admin.setRoles(new HashSet<>(Set.of(roleAdmin, rolePassenger)));
            userRepository.save(admin);
            logger.info("Created Demo Admin: admin@carpool.com / Admin123!");
        }

        // 3. Driver account 1
        User driverJohn = userRepository.findByEmail("driver@carpool.com").orElse(null);
        if (driverJohn == null) {
            driverJohn = new User("driver@carpool.com", passwordEncoder.encode("Driver123!"), "John Driver", "+1 555-0101");
            driverJohn.setRoles(new HashSet<>(Set.of(roleDriver, rolePassenger)));
            driverJohn = userRepository.save(driverJohn);
            logger.info("Created Demo Driver: driver@carpool.com / Driver123!");
        }

        // 4. Passenger account 1
        User passengerAlice = userRepository.findByEmail("passenger@carpool.com").orElse(null);
        if (passengerAlice == null) {
            passengerAlice = new User("passenger@carpool.com", passwordEncoder.encode("Passenger123!"), "Alice Passenger", "+1 555-0102");
            passengerAlice.setRoles(new HashSet<>(Set.of(rolePassenger)));
            passengerAlice = userRepository.save(passengerAlice);
            logger.info("Created Demo Passenger: passenger@carpool.com / Passenger123!");
        }

        // 5. Passenger account 2
        User passengerBob = userRepository.findByEmail("bob@carpool.com").orElse(null);
        if (passengerBob == null) {
            passengerBob = new User("bob@carpool.com", passwordEncoder.encode("Passenger123!"), "Bob Rider", "+1 555-0103");
            passengerBob.setRoles(new HashSet<>(Set.of(rolePassenger)));
            passengerBob = userRepository.save(passengerBob);
        }

        // 6. Vehicles for Driver John
        Vehicle camry = null;
        if (vehicleRepository.findByDriverIdAndActiveTrue(driverJohn.getId()).isEmpty()) {
            camry = new Vehicle(driverJohn, "Toyota", "Camry", 2023, "XYZ-7890", "Silver", 4);
            camry = vehicleRepository.save(camry);
            logger.info("Registered Demo Vehicle: Toyota Camry (XYZ-7890)");
        } else {
            camry = vehicleRepository.findByDriverIdAndActiveTrue(driverJohn.getId()).get(0);
        }

        // 7. Rides & Intermediate Stops
        if (rideRepository.findByDriverIdOrderByDepartureDateDescDepartureTimeDesc(driverJohn.getId()).isEmpty()) {
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            // Ride 1: Downtown to Tech Park via University Campus
            Ride ride1 = new Ride(
                    driverJohn,
                    camry,
                    "Downtown Transit Center",
                    40.7128, -74.0060,
                    "Tech Innovation Park",
                    40.7589, -73.9851,
                    tomorrow,
                    LocalTime.of(8, 30),
                    4,
                    BigDecimal.valueOf(20.00),
                    25.0, // Driver covers 25%, remaining $15.00 split across 4 seats -> $3.75/seat
                    BigDecimal.valueOf(3.75)
            );
            ride1.setNotes("Commute to tech park every morning. Flexible pickup along the corridor!");
            RideStop stop1 = new RideStop(ride1, "University Campus", 40.7295, -73.9965, 1);
            ride1.addStop(stop1);
            rideRepository.save(ride1);

            // Ride 2: Central Station to Airport
            Ride ride2 = new Ride(
                    driverJohn,
                    camry,
                    "Grand Central Station",
                    40.7527, -73.9772,
                    "JFK International Airport",
                    40.6413, -73.7781,
                    tomorrow.plusDays(1),
                    LocalTime.of(17, 0),
                    3,
                    BigDecimal.valueOf(36.00),
                    20.0, // Remaining $28.80 split across 3 seats -> $9.60/seat
                    BigDecimal.valueOf(9.60)
            );
            ride2.setNotes("Luggage trunk space available. Punctual departure.");
            RideStop stop2 = new RideStop(ride2, "Brooklyn Bridge Park", 40.7061, -73.9969, 1);
            ride2.addStop(stop2);
            rideRepository.save(ride2);

            // Ride 3: Completed historical ride with Bob for rating demonstration
            Ride pastRide = new Ride(
                    driverJohn,
                    camry,
                    "Wall Street Financial Hub",
                    40.7061, -74.0092,
                    "Central Park North",
                    40.7950, -73.9550,
                    LocalDate.now().minusDays(2),
                    LocalTime.of(9, 0),
                    4,
                    BigDecimal.valueOf(16.00),
                    0.0,
                    BigDecimal.valueOf(4.00)
            );
            pastRide.setStatus(RideStatus.COMPLETED);
            pastRide.setAvailableSeats(3);
            Ride savedPastRide = rideRepository.save(pastRide);

            // Booking for Bob on past ride
            Booking completedBooking = new Booking(
                    savedPastRide,
                    passengerBob,
                    1,
                    BigDecimal.valueOf(4.00),
                    "Wall Street",
                    "Central Park North",
                    BookingStatus.COMPLETED
            );
            completedBooking.setRated(true);
            Booking savedBooking = bookingRepository.save(completedBooking);

            // 5-star rating from Bob
            Rating rating = new Rating(
                    savedBooking,
                    savedPastRide,
                    passengerBob,
                    driverJohn,
                    5,
                    "Outstanding carpool experience! John was very punctual, polite, and drove safely. Saved me $20 compared to an app cab."
            );
            ratingRepository.save(rating);

            // Sample Welcome Notifications
            notificationRepository.save(new Notification(
                    driverJohn,
                    "Welcome to Smart Carpool!",
                    "Your driver profile and vehicle are configured. You can publish rides and manage bookings anytime.",
                    NotificationType.SYSTEM
            ));

            notificationRepository.save(new Notification(
                    passengerAlice,
                    "Welcome to Smart Carpool!",
                    "Search for rides along your route and experience intelligent, cost-effective carpooling.",
                    NotificationType.SYSTEM
            ));

            logger.info("Successfully seeded demo rides, completed booking, and 5-star review!");
        }
    }

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }
}
