package com.carpool.ridesharing.controller;

import com.carpool.ridesharing.dto.ReportDto;
import com.carpool.ridesharing.entity.Report;
import com.carpool.ridesharing.entity.Ride;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.repository.ReportRepository;
import com.carpool.ridesharing.service.NotificationService;
import com.carpool.ridesharing.service.RideService;
import com.carpool.ridesharing.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final RideService rideService;
    private final NotificationService notificationService;

    public ReportController(ReportRepository reportRepository,
                            UserService userService,
                            RideService rideService,
                            NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.rideService = rideService;
        this.notificationService = notificationService;
    }

    private User getLoggedInUser(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping("/ride/{rideId}")
    public String showReportForm(@PathVariable("rideId") Long rideId, Model model, Principal principal) {
        User user = getLoggedInUser(principal);
        Ride ride = rideService.getRideById(rideId);

        ReportDto reportDto = new ReportDto();
        reportDto.setReportedRideId(rideId);
        reportDto.setReportedUserId(ride.getDriver().getId());

        model.addAttribute("currentUser", user);
        model.addAttribute("ride", ride);
        model.addAttribute("reportDto", reportDto);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));

        return "reports/create";
    }

    @PostMapping("/ride/{rideId}")
    public String submitReport(@PathVariable("rideId") Long rideId,
                               @Valid @ModelAttribute("reportDto") ReportDto dto,
                               BindingResult bindingResult,
                               Principal principal,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        User user = getLoggedInUser(principal);
        Ride ride = rideService.getRideById(rideId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", user);
            model.addAttribute("ride", ride);
            model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));
            return "reports/create";
        }

        Report report = new Report(user, ride.getDriver(), ride, dto.getReason());
        reportRepository.save(report);

        redirectAttributes.addFlashAttribute("successMessage",
                "Your report has been submitted for administrative review. Thank you for helping keep our community safe.");
        return "redirect:/passenger/dashboard";
    }
}
