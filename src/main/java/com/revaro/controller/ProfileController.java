package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.repository.EventRepository;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.EventService;
import com.revaro.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserService userService;
    private final EventRepository eventRepository;
    private final EventService eventService;

    public ProfileController(UserService userService, EventRepository eventRepository, EventService eventService) {
        this.userService = userService;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
    }

    // ── Public profile ────────────────────────────────────────────────────────

    @GetMapping("/profile/{username}")
    public String publicProfile(@PathVariable String username, Model model) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        model.addAttribute("profileUser", user);
        model.addAttribute("revPoints", userService.calculateRevPoints(user));
        model.addAttribute("eventCount", userService.countEventsForUser(user));
        model.addAttribute("profileEvents", eventService.findByCreatorHydrated(user));
        return "user/profile";
    }

    // ── Own profile (redirect to username route) ──────────────────────────────

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String ownProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        return "redirect:/profile/" + principal.getUsername();
    }

    // ── Edit profile ──────────────────────────────────────────────────────────

    @GetMapping("/profile/edit")
    @PreAuthorize("isAuthenticated()")
    public String editProfile(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        User user = userService.findById(principal.getUser().getId())
                .orElseThrow();
        model.addAttribute("user", user);
        return "user/edit-profile";
    }

    @PostMapping("/profile/edit")
    @PreAuthorize("isAuthenticated()")
    public String saveProfile(@AuthenticationPrincipal UserDetailsImpl principal,
                              @RequestParam(required = false) MultipartFile profileImageFile,
                              @RequestParam(required = false) String carYear,
                              @RequestParam(required = false) String carMake,
                              @RequestParam(required = false) String carModel,
                              @RequestParam(required = false) MultipartFile carImageFile,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(principal.getUser().getId()).orElseThrow();
            userService.updateProfile(user, profileImageFile, carYear, carMake, carModel, carImageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not update profile: " + e.getMessage());
        }
        return "redirect:/profile/" + principal.getUsername();
    }

    // ── My events ─────────────────────────────────────────────────────────────

    @GetMapping("/my-events")
    @PreAuthorize("isAuthenticated()")
    public String myEvents(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        User user = userService.findById(principal.getUser().getId()).orElseThrow();
        model.addAttribute("events", eventService.findByCreatorHydrated(user));
        return "user/my-events";
    }
}
