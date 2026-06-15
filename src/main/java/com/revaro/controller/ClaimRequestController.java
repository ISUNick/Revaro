package com.revaro.controller;

import com.revaro.entity.Event;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.ClaimRequestService;
import com.revaro.service.EventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/claims")
public class ClaimRequestController {

    private final ClaimRequestService claimRequestService;
    private final EventService eventService;

    public ClaimRequestController(ClaimRequestService claimRequestService,
                                   EventService eventService) {
        this.claimRequestService = claimRequestService;
        this.eventService = eventService;
    }

    // ── Claim form ────────────────────────────────────────────────────────────

    @GetMapping("/submit/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public String showClaimForm(@PathVariable Long eventId,
                                @AuthenticationPrincipal UserDetailsImpl principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        // Creator can't claim their own event
        if (event.getCreator().getId().equals(principal.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You are already the creator of this event.");
            return "redirect:/events/" + eventId;
        }

        boolean alreadyPending = claimRequestService.hasPendingClaim(
                principal.getUser(), event);

        model.addAttribute("event", event);
        model.addAttribute("alreadyPending", alreadyPending);
        return "claim/form";
    }

    // ── Submit claim ──────────────────────────────────────────────────────────

    @PostMapping("/submit/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public String submitClaim(@PathVariable Long eventId,
                              @RequestParam(required = false) String message,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {

        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        try {
            claimRequestService.submitClaim(principal.getUser(), event, message);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your claim has been submitted! We'll review it shortly.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/events/" + eventId;
    }

    // ── My claims ─────────────────────────────────────────────────────────────

    @GetMapping("/my-claims")
    @PreAuthorize("isAuthenticated()")
    public String myClaims(@AuthenticationPrincipal UserDetailsImpl principal,
                           Model model) {
        model.addAttribute("claims",
                claimRequestService.getClaimsForUser(principal.getUser()));
        return "claim/my-claims";
    }
}
