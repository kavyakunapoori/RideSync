package com.carpool.ridesharing.controller;

import com.carpool.ridesharing.entity.Notification;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.service.NotificationService;
import com.carpool.ridesharing.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    private User getLoggedInUser(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping
    public String notificationsPage(Model model, Principal principal) {
        User user = getLoggedInUser(principal);
        List<Notification> notifications = notificationService.getUserNotifications(user.getId());

        model.addAttribute("currentUser", user);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadNotifs", notificationService.getUnreadCount(user.getId()));

        return "notifications/index";
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable("id") Long id, Principal principal) {
        User user = getLoggedInUser(principal);
        notificationService.markAsRead(id, user.getId());
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(Principal principal) {
        User user = getLoggedInUser(principal);
        notificationService.markAllAsRead(user.getId());
        return "redirect:/notifications";
    }
}
