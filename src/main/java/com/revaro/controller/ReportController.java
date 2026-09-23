package com.revaro.controller;

import com.revaro.entity.Comment;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.CommentService;
import com.revaro.service.EventService;
import com.revaro.service.ReportService;
import com.revaro.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;
    private final EventService eventService;
    private final CommentService commentService;
    private final UserService userService;

    public ReportController(ReportService reportService,
                            EventService eventService,
                            CommentService commentService,
                            UserService userService) {
        this.reportService = reportService;
        this.eventService = eventService;
        this.commentService = commentService;
        this.userService = userService;
    }

    @PostMapping("/event/{id}")
    public String reportEvent(@PathVariable Long id,
                              @RequestParam(required = false) String reason,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        boolean created = reportService.reportEvent(eventService.getById(id), principal.getUser(), reason);
        flashResult(redirectAttributes, created, "this event");
        return "redirect:/events/" + id;
    }

    @PostMapping("/comment/{id}")
    public String reportComment(@PathVariable Long id,
                                @RequestParam(required = false) String reason,
                                @AuthenticationPrincipal UserDetailsImpl principal,
                                RedirectAttributes redirectAttributes) {
        Comment comment = commentService.getById(id);
        boolean created = reportService.reportComment(comment, principal.getUser(), reason);
        flashResult(redirectAttributes, created, "this comment");
        return "redirect:/events/" + comment.getEvent().getId() + "#comments";
    }

    @PostMapping("/user/{username}")
    public String reportUser(@PathVariable String username,
                             @RequestParam(required = false) String reason,
                             @AuthenticationPrincipal UserDetailsImpl principal,
                             RedirectAttributes redirectAttributes) {
        try {
            boolean created = reportService.reportUser(userService.getByUsername(username), principal.getUser(), reason);
            flashResult(redirectAttributes, created, "this user");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile/" + username;
    }

    private void flashResult(RedirectAttributes redirectAttributes, boolean created, String target) {
        if (created) {
            redirectAttributes.addFlashAttribute("successMessage", "Report submitted. An admin will take a look.");
        } else {
            redirectAttributes.addFlashAttribute("infoMessage", "You already reported " + target + ".");
        }
    }
}
