package com.revaro.controller;

import com.revaro.entity.Event;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.ClaimRequestService;
import com.revaro.service.EventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/claims")
@PreAuthorize("isAuthenticated()")
public class ClaimRequestController {

    private final ClaimRequestService claimRequestService;
    private final EventService eventService;

    public ClaimRequestController(ClaimRequestService claimRequestService, EventService eventService) {
        this.claimRequestService = claimRequestService;
        this.eventService = eventService;
    }

    @GetMapping("/submit/{eventId}")
    public String claimForm(@PathVariable Long eventId,
                            @AuthenticationPrincipal UserDetailsImpl principal,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        Event event = eventService.getById(eventId);
        if (event.getCreator().getId().equals(principal.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are already the creator of this event.");
            return "redirect:/events/" + eventId;
        }
        model.addAttribute("event", event);
        model.addAttribute("alreadyPending", claimRequestService.hasPendingClaim(principal.getUser(), event));
        return "claim/form";
    }

    @PostMapping("/submit/{eventId}")
    public String submitClaim(@PathVariable Long eventId,
                              @RequestParam(required = false) String message,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        try {
            claimRequestService.submitClaim(principal.getUser(), eventService.getById(eventId), message);
            redirectAttributes.addFlashAttribute("successMessage", "Claim submitted! We'll review it soon.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/events/" + eventId;
    }

    @GetMapping("/my-claims")
    public String myClaims(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        model.addAttribute("claims", claimRequestService.getClaimsForUser(principal.getUser()));
        return "claim/my-claims";
    }
}
