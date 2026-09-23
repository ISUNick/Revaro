package com.revaro.controller;

import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.enums.RsvpStatus;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.EventService;
import com.revaro.service.NotificationService;
import com.revaro.service.RsvpService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/events/{eventId}/rsvp")
public class RsvpController {

    private final RsvpService rsvpService;
    private final EventService eventService;
    private final NotificationService notificationService;

    public RsvpController(RsvpService rsvpService,
                          EventService eventService,
                          NotificationService notificationService) {
        this.rsvpService = rsvpService;
        this.eventService = eventService;
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public String toggleRsvp(@PathVariable Long eventId,
                             @RequestParam RsvpStatus status,
                             @AuthenticationPrincipal UserDetailsImpl principal,
                             RedirectAttributes redirectAttributes) {
        Event event = eventService.getById(eventId);
        User user = principal.getUser();
        RsvpStatus result = rsvpService.toggleRsvp(user, event, status);

        if (result == null) {
            redirectAttributes.addFlashAttribute("infoMessage", "RSVP removed.");
        } else {
            notificationService.notifyRsvp(user, event, result);
            redirectAttributes.addFlashAttribute("successMessage",
                    result == RsvpStatus.GOING ? "You're marked as going!" : "You're marked as interested!");
        }
        return "redirect:/events/" + eventId;
    }
}
