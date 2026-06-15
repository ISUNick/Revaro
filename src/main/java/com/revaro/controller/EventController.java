package com.revaro.controller;

import com.revaro.dto.EventDto;
import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.enums.EventStatus;
import com.revaro.enums.EventType;
import com.revaro.enums.RsvpStatus;
import com.revaro.enums.SourceType;
import com.revaro.repository.CommentLikeRepository;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.CommentService;
import com.revaro.service.EventService;
import com.revaro.service.RsvpService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final RsvpService rsvpService;
    private final CommentService commentService;
    private final CommentLikeRepository commentLikeRepository;

    public EventController(EventService eventService,
                           RsvpService rsvpService,
                           CommentService commentService,
                           CommentLikeRepository commentLikeRepository) {
        this.eventService = eventService;
        this.rsvpService = rsvpService;
        this.commentService = commentService;
        this.commentLikeRepository = commentLikeRepository;
    }

    private void addFormEnums(Model model) {
        model.addAttribute("eventTypes", EventType.values());
        model.addAttribute("sourceTypes", SourceType.values());
        model.addAttribute("eventStatuses", EventStatus.values());
    }

    // ── Event Detail ──────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String eventDetail(@PathVariable Long id,
                              Model model,
                              @AuthenticationPrincipal UserDetailsImpl principal) {

        Event event = eventService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));

        List<Comment> comments = commentService.getCommentsForEvent(event);

        model.addAttribute("event", event);
        model.addAttribute("comments", comments);

        if (principal != null) {
            User currentUser = principal.getUser();
            model.addAttribute("currentUser", currentUser);

            // Current user's RSVP status
            rsvpService.getUserRsvpStatus(currentUser, event)
                    .ifPresent(s -> model.addAttribute("userRsvpStatus", s));

            // Which comments the current user has liked
            Map<Long, Boolean> likedComments = new HashMap<>();
            for (Comment c : comments) {
                likedComments.put(c.getId(),
                        commentLikeRepository.existsByUserAndComment(currentUser, c));
            }
            model.addAttribute("likedComments", likedComments);
        }

        return "event/detail";
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @GetMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public String createForm(Model model) {
        model.addAttribute("eventDto", new EventDto());
        model.addAttribute("formMode", "create");
        addFormEnums(model);
        return "event/form";
    }

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public String createEvent(@Valid @ModelAttribute("eventDto") EventDto dto,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes,
                              Model model) {

        if (!dto.isPostedByOrganizer()
                && (dto.getOrganizerName() == null || dto.getOrganizerName().isBlank())) {
            bindingResult.rejectValue("organizerName", "required",
                    "Organizer name is required when posting on behalf of another organizer.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            addFormEnums(model);
            return "event/form";
        }

        try {
            Event event = eventService.createEvent(dto, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Event posted successfully!");
            return "redirect:/events/" + event.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("formMode", "create");
            addFormEnums(model);
            return "event/form";
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Image upload failed. Please try a different image.");
            model.addAttribute("formMode", "create");
            addFormEnums(model);
            return "event/form";
        }
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetailsImpl principal,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        Event event = eventService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        User currentUser = principal.getUser();
        if (!event.getCreator().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You don't have permission to edit this event.");
            return "redirect:/events/" + id;
        }

        model.addAttribute("eventDto", eventService.toDto(event));
        model.addAttribute("event", event);
        model.addAttribute("formMode", "edit");
        addFormEnums(model);
        return "event/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String updateEvent(@PathVariable Long id,
                              @Valid @ModelAttribute("eventDto") EventDto dto,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes,
                              Model model) {

        if (!dto.isPostedByOrganizer()
                && (dto.getOrganizerName() == null || dto.getOrganizerName().isBlank())) {
            bindingResult.rejectValue("organizerName", "required",
                    "Organizer name is required when posting on behalf of another organizer.");
        }

        if (bindingResult.hasErrors()) {
            Event event = eventService.findById(id).orElseThrow();
            model.addAttribute("event", event);
            model.addAttribute("formMode", "edit");
            addFormEnums(model);
            return "event/form";
        }

        try {
            eventService.updateEvent(id, dto, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Event updated successfully!");
            return "redirect:/events/" + id;
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/events/" + id;
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Image upload failed. Please try a different image.");
            model.addAttribute("formMode", "edit");
            addFormEnums(model);
            return "event/form";
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteEvent(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEvent(id, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Event deleted.");
            return "redirect:/my-events";
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/events/" + id;
        }
    }
}
