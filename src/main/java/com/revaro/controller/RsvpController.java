package com.revaro.controller;

import com.revaro.entity.Event;
import com.revaro.enums.RsvpStatus;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.EventService;
import com.revaro.service.NotificationService;
import com.revaro.service.RsvpService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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
                             @RequestParam String status,
                             @AuthenticationPrincipal UserDetailsImpl principal,
                             RedirectAttributes redirectAttributes) {

        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        try {
            RsvpStatus requested = RsvpStatus.valueOf(status.toUpperCase());
            RsvpStatus result = rsvpService.toggleRsvp(principal.getUser(), event, requested);

            if (result == null) {
                redirectAttributes.addFlashAttribute("infoMessage", "RSVP removed.");
            } else {
                String label = result == RsvpStatus.GOING ? "Going" : "Interested";
                redirectAttributes.addFlashAttribute("successMessage",
                        "You are marked as " + label + "!");
                // Fire notification
                if (result == RsvpStatus.GOING) {
                    notificationService.notifyRsvpGoing(principal.getUser(), event);
                } else {
                    notificationService.notifyRsvpInterested(principal.getUser(), event);
                }
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid RSVP status.");
        }

        return "redirect:/events/" + eventId;
    }
}
