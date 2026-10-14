package com.carpool.ridesharing.controller;

import com.carpool.ridesharing.dto.RideCreateDto;
import com.carpool.ridesharing.dto.VehicleDto;
import com.carpool.ridesharing.entity.*;
import com.carpool.ridesharing.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/driver")
public class DriverController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final RideService rideService;
    private final BookingService bookingService;
    private final RatingService ratingService;
    private final NotificationService notificationService;

    public DriverController(UserService userService,
                            VehicleService vehicleService,
                            RideService rideService,
                            BookingService bookingService,
                            RatingService ratingService,
                            NotificationService notificationService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.rideService = rideService;
        this.bookingService = bookingService;
        this.ratingService = ratingService;
        this.notificationService = notificationService;
    }

    private User getLoggedInUser(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User driver = getLoggedInUser(principal);
        List<Ride> rides = rideService.getRidesByDriver(driver.getId());
        List<Booking> bookings = bookingService.getDriverBookings(driver.getId());
        List<Vehicle> vehicles = vehicleService.getVehiclesByDriver(driver.getId());

        // Calculate total shared cost contributions collected
        BigDecimal totalCollected = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                .map(Booking::getTotalFare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double avgRating = ratingService.getAverageRatingForDriver(driver.getId());
        long totalReviews = ratingService.getDriverReviews(driver.getId()).size();

        model.addAttribute("currentUser", driver);
        model.addAttribute("rides", rides);
        model.addAttribute("bookings", bookings);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("totalCollected", totalCollected);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(driver.getId()));

        return "driver/dashboard";
    }

    @GetMapping("/vehicles")
    public String vehiclesPage(Model model, Principal principal) {
        User driver = getLoggedInUser(principal);
        List<Vehicle> vehicles = vehicleService.getVehiclesByDriver(driver.getId());

        if (!model.containsAttribute("vehicleDto")) {
            model.addAttribute("vehicleDto", new VehicleDto());
        }
        model.addAttribute("currentUser", driver);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(driver.getId()));

        return "driver/vehicles";
    }

    @PostMapping("/vehicles")
    public String addVehicle(@Valid @ModelAttribute("vehicleDto") VehicleDto dto,
                             BindingResult bindingResult,
                             Principal principal,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        User driver = getLoggedInUser(principal);
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", driver);
            model.addAttribute("vehicles", vehicleService.getVehiclesByDriver(driver.getId()));
            model.addAttribute("unreadNotifs", notificationService.getUnreadCount(driver.getId()));
            return "driver/vehicles";
        }

        try {
            vehicleService.addVehicle(driver, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle added successfully!");
            return "redirect:/driver/vehicles";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/driver/vehicles";
        }
    }

    @GetMapping("/rides/new")
    public String newRidePage(Model model, Principal principal) {
        User driver = getLoggedInUser(principal);
        List<Vehicle> vehicles = vehicleService.getVehiclesByDriver(driver.getId());

        if (vehicles.isEmpty()) {
            model.addAttribute("errorMessage", "You must register at least one vehicle before publishing a ride.");
            model.addAttribute("vehicleDto", new VehicleDto());
            model.addAttribute("currentUser", driver);
            model.addAttribute("vehicles", vehicles);
            model.addAttribute("unreadNotifs", notificationService.getUnreadCount(driver.getId()));
            return "driver/vehicles";
        }

        if (!model.containsAttribute("rideDto")) {
            model.addAttribute("rideDto", new RideCreateDto());
        }
        model.addAttribute("currentUser", driver);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(driver.getId()));

        return "driver/rides-new";
    }

    @PostMapping("/rides/new")
    public String publishRide(@Valid @ModelAttribute("rideDto") RideCreateDto dto,
                              BindingResult bindingResult,
                              Principal principal,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        User driver = getLoggedInUser(principal);
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", driver);
            model.addAttribute("vehicles", vehicleService.getVehiclesByDriver(driver.getId()));
            model.addAttribute("unreadNotifs", notificationService.getUnreadCount(driver.getId()));
            return "driver/rides-new";
        }

        try {
            Ride publishedRide = rideService.publishRide(driver, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Ride published successfully! Offered " + publishedRide.getTotalSeats() +
                            " seat(s) at $" + publishedRide.getPassengerCost() + " each.");
            return "redirect:/driver/rides/" + publishedRide.getId();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("currentUser", driver);
            model.addAttribute("vehicles", vehicleService.getVehiclesByDriver(driver.getId()));
            model.addAttribute("unreadNotifs", notificationService.getUnreadCount(driver.getId()));
            return "driver/rides-new";
        }
    }

    @GetMapping("/rides/{id}")
    public String rideDetails(@PathVariable("id") Long rideId, Model model, Principal principal) {
        User driver = getLoggedInUser(principal);
        Ride ride = rideService.getRideById(rideId);
        List<Booking> bookings = bookingService.getBookingsForRide(rideId);

        model.addAttribute("currentUser", driver);
        model.addAttribute("ride", ride);
        model.addAttribute("bookings", bookings);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(driver.getId()));

        return "driver/ride-details";
    }

    @PostMapping("/rides/{id}/complete")
    public String completeRide(@PathVariable("id") Long rideId,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User driver = getLoggedInUser(principal);
        try {
            rideService.completeRide(rideId, driver.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Ride marked as completed! Passengers were notified to leave reviews.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/driver/rides/" + rideId;
    }

    @PostMapping("/rides/{id}/cancel")
    public String cancelRide(@PathVariable("id") Long rideId,
                             @RequestParam(value = "reason", required = false) String reason,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        User driver = getLoggedInUser(principal);
        try {
            rideService.cancelRide(rideId, driver.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Ride cancelled. Passengers were notified.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/driver/rides/" + rideId;
    }

    @PostMapping("/bookings/{id}/accept")
    public String acceptBooking(@PathVariable("id") Long bookingId,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        User driver = getLoggedInUser(principal);
        try {
            bookingService.acceptBooking(bookingId, driver.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Booking approved!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/bookings/{id}/reject")
    public String rejectBooking(@PathVariable("id") Long bookingId,
                                @RequestParam(value = "reason", required = false) String reason,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        User driver = getLoggedInUser(principal);
        try {
            bookingService.rejectBooking(bookingId, driver.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Booking rejected and seats refunded.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/driver/dashboard";
    }

    @GetMapping("/reviews")
    public String driverReviews(Model model, Principal principal) {
        User driver = getLoggedInUser(principal);
        List<Rating> reviews = ratingService.getDriverReviews(driver.getId());
        Double avgRating = ratingService.getAverageRatingForDriver(driver.getId());

        model.addAttribute("currentUser", driver);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(driver.getId()));

        return "driver/reviews";
    }
}
