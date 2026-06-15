package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.enums.ClaimStatus;
import com.revaro.enums.Role;
import com.revaro.repository.CommentRepository;
import com.revaro.repository.EventRepository;
import com.revaro.repository.UserRepository;
import com.revaro.service.ClaimRequestService;
import com.revaro.service.EventService;
import com.revaro.security.UserDetailsImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ClaimRequestService claimRequestService;
    private final EventService eventService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentRepository commentRepository;

    public AdminController(ClaimRequestService claimRequestService,
                           EventService eventService,
                           UserRepository userRepository,
                           EventRepository eventRepository,
                           CommentRepository commentRepository) {
        this.claimRequestService = claimRequestService;
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.commentRepository = commentRepository;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("pendingClaimsCount", claimRequestService.countPending());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalEvents", eventRepository.count());
        model.addAttribute("recentClaims", claimRequestService.getPendingClaims());
        return "admin/dashboard";
    }

    // ── Claims ────────────────────────────────────────────────────────────────

    @GetMapping("/claims")
    public String claims(Model model) {
        model.addAttribute("pendingClaims", claimRequestService.getPendingClaims());
        model.addAttribute("allClaims", claimRequestService.getAllClaims());
        return "admin/claims";
    }

    @PostMapping("/claims/{id}/approve")
    public String approveClaim(@PathVariable Long id,
                               @RequestParam(required = false) String adminNotes,
                               @AuthenticationPrincipal UserDetailsImpl principal,
                               RedirectAttributes redirectAttributes) {
        try {
            claimRequestService.reviewClaim(id, principal.getUser(), ClaimStatus.APPROVED, adminNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Claim approved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/claims";
    }

    @PostMapping("/claims/{id}/reject")
    public String rejectClaim(@PathVariable Long id,
                              @RequestParam(required = false) String adminNotes,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        try {
            claimRequestService.reviewClaim(id, principal.getUser(), ClaimStatus.REJECTED, adminNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Claim rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/claims";
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll(Sort.by("createdAt").descending()));
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-admin")
    public String toggleAdmin(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        if (id.equals(principal.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot change your own admin status.");
            return "redirect:/admin/users";
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setRole(user.getRole() == Role.ADMIN ? Role.USER : Role.ADMIN);
        userRepository.save(user);
        String action = user.getRole() == Role.ADMIN ? "promoted to admin" : "demoted to user";
        redirectAttributes.addFlashAttribute("successMessage", user.getUsername() + " " + action + ".");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetailsImpl principal,
                             RedirectAttributes redirectAttributes) {
        if (id.equals(principal.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot delete your own account.");
            return "redirect:/admin/users";
        }
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));
            userRepository.delete(user);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @GetMapping("/events")
    public String events(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("events",
                eventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 20)));
        return "admin/events";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEvent(id, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Event deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/events";
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    @GetMapping("/comments")
    public String comments(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("comments",
                commentRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(page, 30)));
        return "admin/comments";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam(required = false) String returnUrl,
                                RedirectAttributes redirectAttributes) {
        try {
            commentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Comment deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete comment: " + e.getMessage());
        }
        // Return to the event page if we have the URL, otherwise admin dashboard
        return "redirect:" + (returnUrl != null && !returnUrl.isBlank() ? returnUrl : "/admin");
    }
}
