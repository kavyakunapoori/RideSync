package com.carpool.ridesharing.controller;

import com.carpool.ridesharing.dto.AdminDashboardStatsDto;
import com.carpool.ridesharing.dto.RideSearchDto;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.service.AdminService;
import com.carpool.ridesharing.service.NotificationService;
import com.carpool.ridesharing.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDate;

@Controller
public class HomeController {

    private final AdminService adminService;
    private final UserService userService;
    private final NotificationService notificationService;

    public HomeController(AdminService adminService, UserService userService, NotificationService notificationService) {
        this.adminService = adminService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping("/")
    public String index(Model model, Principal principal) {
        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("searchDto", new RideSearchDto("", "", LocalDate.now()));

        if (principal != null) {
            userService.findByEmail(principal.getName()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
                model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));
            });
        }
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model, Principal principal) {
        if (principal != null) {
            userService.findByEmail(principal.getName()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
                model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));
            });
        }
        return "about";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isDriver = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else if (isDriver) {
            return "redirect:/driver/dashboard";
        } else {
            return "redirect:/passenger/dashboard";
        }
    }
}
