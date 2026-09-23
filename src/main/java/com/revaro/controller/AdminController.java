package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.enums.ClaimStatus;
import com.revaro.enums.ReportStatus;
import com.revaro.repository.CommentRepository;
import com.revaro.repository.EventRepository;
import com.revaro.repository.ReportRepository;
import com.revaro.repository.UserRepository;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.AdminService;
import com.revaro.service.ClaimRequestService;
import com.revaro.service.EventService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final int PAGE_SIZE = 20;
    private static final int COMMENTS_PAGE_SIZE = 30;

    private final AdminService adminService;
    private final ClaimRequestService claimRequestService;
    private final EventService eventService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;

    public AdminController(AdminService adminService,
                           ClaimRequestService claimRequestService,
                           EventService eventService,
                           UserRepository userRepository,
                           EventRepository eventRepository,
                           CommentRepository commentRepository,
                           ReportRepository reportRepository) {
        this.adminService = adminService;
        this.claimRequestService = claimRequestService;
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.commentRepository = commentRepository;
        this.reportRepository = reportRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("pendingClaimsCount", claimRequestService.countPending());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalEvents", eventRepository.count());
        model.addAttribute("totalComments", commentRepository.count());
        model.addAttribute("pendingReports", reportRepository.countByStatus(ReportStatus.PENDING));
        model.addAttribute("recentClaims", claimRequestService.getPendingClaims());
        return "admin/dashboard";
    }

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
        return reviewClaim(id, ClaimStatus.APPROVED, adminNotes, principal.getUser(), redirectAttributes);
    }

    @PostMapping("/claims/{id}/reject")
    public String rejectClaim(@PathVariable Long id,
                              @RequestParam(required = false) String adminNotes,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        return reviewClaim(id, ClaimStatus.REJECTED, adminNotes, principal.getUser(), redirectAttributes);
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll(Sort.by("createdAt").descending()));
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-admin")
    public String toggleAdmin(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = adminService.toggleAdmin(id, principal.getUser());
            String change = user.isAdmin() ? " is now an admin." : " is no longer an admin.";
            redirectAttributes.addFlashAttribute("successMessage", user.getUsername() + change);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetailsImpl principal,
                             RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteUser(id, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "User deleted.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/events")
    public String events(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("events", eventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, PAGE_SIZE)));
        return "admin/events";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        eventService.deleteEvent(id, principal.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "Event deleted.");
        return "redirect:/admin/events";
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) ReportStatus status,
                          Model model) {
        PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("reports", status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                : reportRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest));
        model.addAttribute("status", status == null ? null : status.name());
        model.addAttribute("pendingCount", reportRepository.countByStatus(ReportStatus.PENDING));
        return "admin/reports";
    }

    @PostMapping("/reports/{id}/dismiss")
    public String dismissReport(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetailsImpl principal,
                                RedirectAttributes redirectAttributes) {
        adminService.dismissReport(id, principal.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "Report dismissed.");
        return "redirect:/admin/reports";
    }

    @PostMapping("/reports/{id}/delete-content")
    public String resolveReport(@PathVariable Long id,
                                @RequestParam String action,
                                @AuthenticationPrincipal UserDetailsImpl principal,
                                RedirectAttributes redirectAttributes) {
        try {
            adminService.resolveReport(id, action, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Done. The report is marked reviewed.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/reports";
    }

    @GetMapping("/comments")
    public String comments(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("comments",
                commentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, COMMENTS_PAGE_SIZE)));
        return "admin/comments";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteComment(id);
        redirectAttributes.addFlashAttribute("successMessage", "Comment deleted.");
        return "redirect:/admin/comments";
    }

    private String reviewClaim(Long id, ClaimStatus decision, String adminNotes, User admin,
                               RedirectAttributes redirectAttributes) {
        try {
            claimRequestService.reviewClaim(id, admin, decision, adminNotes);
            String result = decision == ClaimStatus.APPROVED ? "Claim approved." : "Claim rejected.";
            redirectAttributes.addFlashAttribute("successMessage", result);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/claims";
    }
}
