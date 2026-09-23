package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetailsImpl principal,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        User user = principal.getUser();
        // Loaded before marking them read so the page can still highlight what's new
        model.addAttribute("notifications", notificationService.getNotifications(user, page));
        notificationService.markAllAsRead(user);
        return "notifications";
    }
}
