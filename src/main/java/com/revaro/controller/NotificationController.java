package com.revaro.controller;

import com.revaro.security.UserDetailsImpl;
import com.revaro.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String notifications(@AuthenticationPrincipal UserDetailsImpl principal,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        if (principal == null) return "redirect:/login";
        var user = principal.getUser();
        model.addAttribute("notifications",
                notificationService.getAllNotifications(user, page));
        notificationService.markAllAsRead(user);
        return "notifications";
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public String markRead(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetailsImpl principal) {
        if (principal != null) {
            notificationService.markAsRead(id, principal.getUser());
        }
        return "ok";
    }

    @PostMapping("/read-all")
    public String markAllRead(@AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        if (principal != null) {
            notificationService.markAllAsRead(principal.getUser());
        }
        return "redirect:/notifications";
    }
}
