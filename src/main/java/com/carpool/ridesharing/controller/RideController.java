package com.carpool.ridesharing.controller;

import com.carpool.ridesharing.dto.BookingRequestDto;
import com.carpool.ridesharing.entity.Booking;
import com.carpool.ridesharing.entity.Ride;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
@RequestMapping("/rides")
public class RideController {

    private final RideService rideService;
    private final BookingService bookingService;
    private final UserService userService;
    private final RatingService ratingService;
    private final NotificationService notificationService;
    private final FareSharingService fareSharingService;

    public RideController(RideService rideService,
                          BookingService bookingService,
                          UserService userService,
                          RatingService ratingService,
                          NotificationService notificationService,
                          FareSharingService fareSharingService) {
        this.rideService = rideService;
        this.bookingService = bookingService;
        this.userService = userService;
        this.ratingService = ratingService;
        this.notificationService = notificationService;
        this.fareSharingService = fareSharingService;
    }

    private User getLoggedInUser(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping("/{id}")
    public String viewRideDetails(@PathVariable("id") Long id, Model model, Principal principal) {
        Ride ride = rideService.getRideById(id);
        Double driverRating = ratingService.getAverageRatingForDriver(ride.getDriver().getId());
        long ratingCount = ratingService.getDriverReviews(ride.getDriver().getId()).size();

        model.addAttribute("ride", ride);
        model.addAttribute("driverRating", driverRating);
        model.addAttribute("ratingCount", ratingCount);

        if (principal != null) {
            User user = getLoggedInUser(principal);
            model.addAttribute("currentUser", user);
            model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));
            model.addAttribute("isDriverOfThisRide", ride.getDriver().getId().equals(user.getId()));
        }

        return "passenger/ride-details";
    }

    @GetMapping("/{id}/book")
    public String showBookingConfirmPage(@PathVariable("id") Long id,
                                         @RequestParam(value = "seats", defaultValue = "1") int seats,
                                         Model model,
                                         Principal principal) {
        User user = getLoggedInUser(principal);
        Ride ride = rideService.getRideById(id);

        if (ride.getDriver().getId().equals(user.getId())) {
            model.addAttribute("errorMessage", "You cannot book seats on your own ride.");
            return "redirect:/rides/" + id;
        }

        BigDecimal seatCost = ride.getPassengerCost();
        BigDecimal totalCost = seatCost.multiply(BigDecimal.valueOf(seats));
        BigDecimal estimatedSoloCost = fareSharingService.calculateEstimatedSoloFare(totalCost);
        BigDecimal savings = estimatedSoloCost.subtract(totalCost);

        BookingRequestDto bookingDto = new BookingRequestDto(ride.getId(), seats);
        bookingDto.setPickupLocation(ride.getSourceAddress());
        bookingDto.setDropoffLocation(ride.getDestinationAddress());

        model.addAttribute("currentUser", user);
        model.addAttribute("ride", ride);
        model.addAttribute("seats", seats);
        model.addAttribute("totalCost", totalCost);
        model.addAttribute("estimatedSoloCost", estimatedSoloCost);
        model.addAttribute("savings", savings);
        model.addAttribute("bookingDto", bookingDto);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));

        return "passenger/booking-confirm";
    }

    @PostMapping("/{id}/book")
    public String processBooking(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("bookingDto") BookingRequestDto dto,
                                 BindingResult bindingResult,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        User user = getLoggedInUser(principal);
        dto.setRideId(id);

        if (bindingResult.hasErrors()) {
            return showBookingConfirmPage(id, dto.getSeatsBooked(), model, principal);
        }

        try {
            Booking booking = bookingService.createBooking(user, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reservation confirmed! Booked " + booking.getSeatsBooked() +
                            " seat(s) for $" + booking.getTotalFare() + ".");
            return "redirect:/passenger/bookings";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/rides/" + id;
        }
    }
}
