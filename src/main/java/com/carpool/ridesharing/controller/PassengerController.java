package com.carpool.ridesharing.controller;

import com.carpool.ridesharing.dto.RideMatchResultDto;
import com.carpool.ridesharing.dto.RideSearchDto;
import com.carpool.ridesharing.entity.Booking;
import com.carpool.ridesharing.entity.BookingStatus;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/passenger")
public class PassengerController {

    private final UserService userService;
    private final BookingService bookingService;
    private final SmartRideMatchingService matchingService;
    private final FareSharingService fareSharingService;
    private final NotificationService notificationService;

    public PassengerController(UserService userService,
                               BookingService bookingService,
                               SmartRideMatchingService matchingService,
                               FareSharingService fareSharingService,
                               NotificationService notificationService) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.matchingService = matchingService;
        this.fareSharingService = fareSharingService;
        this.notificationService = notificationService;
    }

    private User getLoggedInUser(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User user = getLoggedInUser(principal);
        List<Booking> bookings = bookingService.getPassengerBookings(user.getId());

        long activeCount = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                .count();

        long completedCount = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();

        // Calculate cumulative savings
        BigDecimal totalSpent = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalFare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSavings = fareSharingService.calculateEstimatedPassengerSavings(totalSpent);
        double co2SavedKg = Math.round((completedCount + activeCount) * 3.24 * 10.0) / 10.0;

        model.addAttribute("currentUser", user);
        model.addAttribute("bookings", bookings);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("totalSavings", totalSavings);
        model.addAttribute("co2SavedKg", co2SavedKg);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));
        model.addAttribute("searchDto", new RideSearchDto("", "", LocalDate.now()));

        return "passenger/dashboard";
    }

    @GetMapping("/search")
    public String searchRides(@ModelAttribute("searchDto") RideSearchDto searchDto,
                              Model model,
                              Principal principal) {
        if (principal != null) {
            User user = getLoggedInUser(principal);
            model.addAttribute("currentUser", user);
            model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));
        }

        if (searchDto.getDepartureDate() == null) {
            searchDto.setDepartureDate(LocalDate.now());
        }

        List<RideMatchResultDto> matches = matchingService.findMatchingRides(searchDto);
        model.addAttribute("matches", matches);
        model.addAttribute("searchDto", searchDto);
        model.addAttribute("hasSearched", searchDto.getSourceAddress() != null && !searchDto.getSourceAddress().isBlank());

        return "passenger/search";
    }

    @GetMapping("/bookings")
    public String myBookings(Model model, Principal principal) {
        User user = getLoggedInUser(principal);
        List<Booking> bookings = bookingService.getPassengerBookings(user.getId());

        model.addAttribute("currentUser", user);
        model.addAttribute("bookings", bookings);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));

        return "passenger/bookings";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable("id") Long bookingId,
                                @RequestParam(value = "reason", required = false) String reason,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        User user = getLoggedInUser(principal);
        try {
            bookingService.cancelBooking(bookingId, user.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully. Seats have been returned to the ride.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/passenger/bookings";
    }

    @GetMapping("/savings")
    public String savings(Model model, Principal principal) {
        User user = getLoggedInUser(principal);
        List<Booking> bookings = bookingService.getPassengerBookings(user.getId());

        BigDecimal totalSpent = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalFare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soloFare = fareSharingService.calculateEstimatedSoloFare(totalSpent);
        BigDecimal totalSavings = fareSharingService.calculateEstimatedPassengerSavings(totalSpent);
        long tripCount = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        double co2SavedKg = Math.round(tripCount * 3.24 * 10.0) / 10.0;

        model.addAttribute("currentUser", user);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("soloFare", soloFare);
        model.addAttribute("totalSavings", totalSavings);
        model.addAttribute("tripCount", tripCount);
        model.addAttribute("co2SavedKg", co2SavedKg);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));

        return "passenger/savings";
    }
}
