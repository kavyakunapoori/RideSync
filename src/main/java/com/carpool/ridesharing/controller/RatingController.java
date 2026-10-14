package com.carpool.ridesharing.controller;

import com.carpool.ridesharing.dto.RatingDto;
import com.carpool.ridesharing.entity.Booking;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.service.BookingService;
import com.carpool.ridesharing.service.NotificationService;
import com.carpool.ridesharing.service.RatingService;
import com.carpool.ridesharing.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final BookingService bookingService;
    private final UserService userService;
    private final NotificationService notificationService;

    public RatingController(RatingService ratingService,
                            BookingService bookingService,
                            UserService userService,
                            NotificationService notificationService) {
        this.ratingService = ratingService;
        this.bookingService = bookingService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    private User getLoggedInUser(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping("/booking/{bookingId}")
    public String showRateForm(@PathVariable("bookingId") Long bookingId,
                               Model model,
                               Principal principal) {
        User user = getLoggedInUser(principal);
        Booking booking = bookingService.getBookingById(bookingId);

        if (!booking.getPassenger().getId().equals(user.getId())) {
            return "redirect:/passenger/bookings";
        }

        RatingDto ratingDto = new RatingDto(bookingId, 5, "");

        model.addAttribute("currentUser", user);
        model.addAttribute("booking", booking);
        model.addAttribute("ratingDto", ratingDto);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));

        return "ratings/rate-driver";
    }

    @PostMapping("/booking/{bookingId}")
    public String submitRating(@PathVariable("bookingId") Long bookingId,
                               @Valid @ModelAttribute("ratingDto") RatingDto dto,
                               BindingResult bindingResult,
                               Principal principal,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        User user = getLoggedInUser(principal);
        dto.setBookingId(bookingId);

        if (bindingResult.hasErrors()) {
            Booking booking = bookingService.getBookingById(bookingId);
            model.addAttribute("currentUser", user);
            model.addAttribute("booking", booking);
            model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));
            return "ratings/rate-driver";
        }

        try {
            ratingService.submitRating(user, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you! Your rating and feedback have been submitted.");
            return "redirect:/passenger/bookings";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/ratings/booking/" + bookingId;
        }
    }
}
