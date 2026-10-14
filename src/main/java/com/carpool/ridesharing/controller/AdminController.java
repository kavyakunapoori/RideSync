package com.carpool.ridesharing.controller;

import com.carpool.ridesharing.dto.AdminDashboardStatsDto;
import com.carpool.ridesharing.entity.Booking;
import com.carpool.ridesharing.entity.Report;
import com.carpool.ridesharing.entity.Ride;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.service.AdminService;
import com.carpool.ridesharing.service.NotificationService;
import com.carpool.ridesharing.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final NotificationService notificationService;

    public AdminController(AdminService adminService,
                           UserService userService,
                           NotificationService notificationService) {
        this.adminService = adminService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    private User getLoggedInAdmin(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User admin = getLoggedInAdmin(principal);
        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        List<Report> reports = adminService.getAllReports();

        model.addAttribute("currentUser", admin);
        model.addAttribute("stats", stats);
        model.addAttribute("recentReports", reports);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(admin.getId()));

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model, Principal principal) {
        User admin = getLoggedInAdmin(principal);
        List<User> users = adminService.getAllUsers();

        model.addAttribute("currentUser", admin);
        model.addAttribute("users", users);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(admin.getId()));

        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long id,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(principal);
        if (admin.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Administrators cannot deactivate their own account.");
            return "redirect:/admin/users";
        }

        boolean active = userService.toggleUserStatus(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "User account has been " + (active ? "activated" : "deactivated") + ".");
        return "redirect:/admin/users";
    }

    @GetMapping("/rides")
    public String rides(Model model, Principal principal) {
        User admin = getLoggedInAdmin(principal);
        List<Ride> rides = adminService.getAllRides();

        model.addAttribute("currentUser", admin);
        model.addAttribute("rides", rides);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(admin.getId()));

        return "admin/rides";
    }

    @GetMapping("/bookings")
    public String bookings(Model model, Principal principal) {
        User admin = getLoggedInAdmin(principal);
        List<Booking> bookings = adminService.getAllBookings();

        model.addAttribute("currentUser", admin);
        model.addAttribute("bookings", bookings);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(admin.getId()));

        return "admin/bookings";
    }

    @GetMapping("/reports")
    public String reports(Model model, Principal principal) {
        User admin = getLoggedInAdmin(principal);
        List<Report> reports = adminService.getAllReports();

        model.addAttribute("currentUser", admin);
        model.addAttribute("reports", reports);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(admin.getId()));

        return "admin/reports";
    }

    @PostMapping("/reports/{id}/resolve")
    public String resolveReport(@PathVariable("id") Long id,
                                @RequestParam(value = "adminNotes", required = false) String adminNotes,
                                RedirectAttributes redirectAttributes) {
        try {
            adminService.resolveReport(id, adminNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Report resolved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/reports";
    }
}
