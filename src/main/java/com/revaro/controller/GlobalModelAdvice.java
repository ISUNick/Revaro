package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.NotificationService;
import com.revaro.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects currentNavUser and unread notification count into every page's model.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;
    private final NotificationService notificationService;

    public GlobalModelAdvice(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("currentNavUser")
    public User currentNavUser(@AuthenticationPrincipal UserDetailsImpl principal) {
        if (principal == null) return null;
        return userService.findById(principal.getUser().getId()).orElse(null);
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(@AuthenticationPrincipal UserDetailsImpl principal) {
        if (principal == null) return 0;
        try {
            return notificationService.getUnreadCount(principal.getUser());
        } catch (Exception e) {
            return 0;
        }
    }
}
