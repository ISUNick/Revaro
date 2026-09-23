package com.revaro.controller;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.CommentService;
import com.revaro.service.EventService;
import com.revaro.service.NotificationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/events/{eventId}/comments")
@PreAuthorize("isAuthenticated()")
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
    public String addComment(@PathVariable Long eventId,
                             @RequestParam String content,
                             @AuthenticationPrincipal UserDetailsImpl principal,
                             RedirectAttributes redirectAttributes) {
        Event event = eventService.getById(eventId);
        User author = principal.getUser();
        try {
            Comment comment = commentService.addComment(author, event, content);
            notificationService.notifyCommentPosted(author, comment);
            return "redirect:/events/" + eventId + "#comment-" + comment.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/events/" + eventId + "#comments";
        }
    }

    @PostMapping("/{commentId}/delete")
    public String deleteComment(@PathVariable Long eventId,
                                @PathVariable Long commentId,
                                @AuthenticationPrincipal UserDetailsImpl principal,
                                RedirectAttributes redirectAttributes) {
        try {
            commentService.deleteComment(commentId, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Comment deleted.");
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/events/" + eventId + "#comments";
    }

    @PostMapping("/{commentId}/like")
    public String toggleLike(@PathVariable Long eventId,
                             @PathVariable Long commentId,
                             @AuthenticationPrincipal UserDetailsImpl principal) {
        Comment comment = commentService.getById(commentId);
        if (commentService.toggleLike(principal.getUser(), comment)) {
            notificationService.notifyCommentLiked(principal.getUser(), comment);
        }
        return "redirect:/events/" + eventId + "#comment-" + commentId;
    }
}
