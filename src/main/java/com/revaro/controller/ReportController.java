package com.revaro.controller;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import com.revaro.entity.Report;
import com.revaro.entity.User;
import com.revaro.enums.ReportStatus;
import com.revaro.enums.ReportType;
import com.revaro.repository.CommentRepository;
import com.revaro.repository.EventRepository;
import com.revaro.repository.ReportRepository;
import com.revaro.repository.UserRepository;
import com.revaro.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/report")
public class ReportController {

    private final ReportRepository reportRepository;
    private final EventRepository eventRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public ReportController(ReportRepository reportRepository,
                            EventRepository eventRepository,
                            CommentRepository commentRepository,
                            UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.eventRepository = eventRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/event/{id}")
    public String reportEvent(@PathVariable Long id,
                              @RequestParam(required = false) String reason,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) { redirectAttributes.addFlashAttribute("errorMessage", "Event not found."); return "redirect:/"; }

        if (reportRepository.existsByReporterIdAndReportedEventId(principal.getUser().getId(), id)) {
            redirectAttributes.addFlashAttribute("infoMessage", "You have already reported this event.");
            return "redirect:/events/" + id;
        }

        Report report = new Report();
        report.setReporter(principal.getUser());
        report.setReportType(ReportType.EVENT);
        report.setReportedEvent(event);
        report.setReason(reason);
        report.setStatus(ReportStatus.PENDING);
        reportRepository.save(report);

        redirectAttributes.addFlashAttribute("successMessage", "Report submitted. Our team will review it.");
        return "redirect:/events/" + id;
    }

    @PostMapping("/comment/{id}")
    public String reportComment(@PathVariable Long id,
                                @RequestParam(required = false) String reason,
                                @RequestParam(required = false) String returnUrl,
                                @AuthenticationPrincipal UserDetailsImpl principal,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        Comment comment = commentRepository.findById(id).orElse(null);
        if (comment == null) { redirectAttributes.addFlashAttribute("errorMessage", "Comment not found."); return "redirect:/"; }

        if (reportRepository.existsByReporterIdAndReportedCommentId(principal.getUser().getId(), id)) {
            redirectAttributes.addFlashAttribute("infoMessage", "You have already reported this comment.");
            return "redirect:" + (returnUrl != null ? returnUrl : "/");
        }

        Report report = new Report();
        report.setReporter(principal.getUser());
        report.setReportType(ReportType.COMMENT);
        report.setReportedComment(comment);
        report.setReason(reason);
        report.setStatus(ReportStatus.PENDING);
        reportRepository.save(report);

        redirectAttributes.addFlashAttribute("successMessage", "Comment reported. Our team will review it.");
        return "redirect:" + (returnUrl != null ? returnUrl : "/");
    }

    @PostMapping("/user/{username}")
    public String reportUser(@PathVariable String username,
                             @RequestParam(required = false) String reason,
                             @AuthenticationPrincipal UserDetailsImpl principal,
                             RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User target = userRepository.findByUsername(username).orElse(null);
        if (target == null) { redirectAttributes.addFlashAttribute("errorMessage", "User not found."); return "redirect:/"; }

        if (target.getId().equals(principal.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot report yourself.");
            return "redirect:/profile/" + username;
        }

        if (reportRepository.existsByReporterIdAndReportedUserId(principal.getUser().getId(), target.getId())) {
            redirectAttributes.addFlashAttribute("infoMessage", "You have already reported this user.");
            return "redirect:/profile/" + username;
        }

        Report report = new Report();
        report.setReporter(principal.getUser());
        report.setReportType(ReportType.USER);
        report.setReportedUser(target);
        report.setReason(reason);
        report.setStatus(ReportStatus.PENDING);
        reportRepository.save(report);

        redirectAttributes.addFlashAttribute("successMessage", "User reported. Our team will review it.");
        return "redirect:/profile/" + username;
    }
}
