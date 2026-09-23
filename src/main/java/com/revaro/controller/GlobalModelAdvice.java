package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.NotificationService;
import com.revaro.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Adds what the navbar needs to every page
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;
    private final NotificationService notificationService;

    public GlobalModelAdvice(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    // Loaded fresh instead of using the session copy so a new profile picture shows right away
    @ModelAttribute("currentNavUser")
    public User currentNavUser(@AuthenticationPrincipal UserDetailsImpl principal) {
        return principal == null ? null : userService.findById(principal.getId()).orElse(null);
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(@AuthenticationPrincipal UserDetailsImpl principal) {
        return principal == null ? 0 : notificationService.getUnreadCount(principal.getUser());
    }
}
