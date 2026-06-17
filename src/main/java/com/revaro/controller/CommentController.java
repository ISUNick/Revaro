package com.revaro.controller;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.CommentService;
import com.revaro.service.EventService;
import com.revaro.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/events/{eventId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final EventService eventService;
    private final NotificationService notificationService;

    public CommentController(CommentService commentService,
                             EventService eventService,
                             NotificationService notificationService) {
        this.commentService = commentService;
        this.eventService = eventService;
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public String addComment(@PathVariable Long eventId,
                             @RequestParam String content,
                             @AuthenticationPrincipal UserDetailsImpl principal,
                             RedirectAttributes redirectAttributes) {

        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        try {
            commentService.addComment(principal.getUser(), event, content);
            notificationService.notifyComment(principal.getUser(), event);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/events/" + eventId + "#comments";
    }

    @PostMapping("/{commentId}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteComment(@PathVariable Long eventId,
                                @PathVariable Long commentId,
                                @AuthenticationPrincipal UserDetailsImpl principal,
                                RedirectAttributes redirectAttributes) {
        try {
            commentService.deleteComment(commentId, principal.getUser());
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/events/" + eventId + "#comments";
    }

    @PostMapping("/{commentId}/like")
    @PreAuthorize("isAuthenticated()")
    public String likeComment(@PathVariable Long eventId,
                              @PathVariable Long commentId,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {

        Comment comment = commentService.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found."));

        boolean liked = commentService.toggleLike(principal.getUser(), comment);
        if (liked) {
            notificationService.notifyCommentLiked(principal.getUser(), comment);
        }

        return "redirect:/events/" + eventId + "#comment-" + commentId;
    }
}
