package com.revaro.controller;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.repository.UserRepository;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.CommentService;
import com.revaro.service.EventService;
import com.revaro.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/events/{eventId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final EventService eventService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+)");

    public CommentController(CommentService commentService,
                             EventService eventService,
                             NotificationService notificationService,
                             UserRepository userRepository) {
        this.commentService = commentService;
        this.eventService = eventService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public String addComment(@PathVariable Long eventId,
                             @RequestParam String content,
                             @AuthenticationPrincipal UserDetailsImpl principal,
                             RedirectAttributes redirectAttributes) {

        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        User actor = principal.getUser();

        try {
            commentService.addComment(actor, event, content);

            // Parse @mentions first — collect who gets mentioned
            java.util.Set<Long> mentionedUserIds = new java.util.HashSet<>();
            Matcher matcher = MENTION_PATTERN.matcher(content);
            while (matcher.find()) {
                String mentionedUsername = matcher.group(1);
                userRepository.findByUsername(mentionedUsername).ifPresent(mentionedUser -> {
                    mentionedUserIds.add(mentionedUser.getId());
                    notificationService.notifyMention(actor, mentionedUser, event);
                });
            }

            // Notify event owner of new comment ONLY if they weren't already
            // notified via a mention — avoids double notification on their own post
            if (!mentionedUserIds.contains(event.getCreator().getId())) {
                notificationService.notifyComment(actor, event);
            }

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

        User actor = principal.getUser();
        boolean liked = commentService.toggleLike(actor, comment);
        if (liked) {
            notificationService.notifyCommentLiked(actor, comment);
        }

        return "redirect:/events/" + eventId + "#comment-" + commentId;
    }

    // ── Autocomplete endpoint for @ mentions ──────────────────────────────────
    @GetMapping("/mention-search")
    @ResponseBody
    public java.util.List<java.util.Map<String, String>> mentionSearch(
            @RequestParam String q) {
        if (q == null || q.length() < 1) return java.util.List.of();
        return userRepository.findByUsernameContainingIgnoreCase(q)
                .stream()
                .limit(6)
                .map(u -> java.util.Map.of(
                        "username", u.getUsername(),
                        "avatar", u.getProfileImage() != null ? u.getProfileImage() : ""
                ))
                .toList();
    }
}
