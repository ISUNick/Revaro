package com.revaro.controller;

import com.revaro.dto.EventDto;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.enums.EventStatus;
import com.revaro.enums.EventType;
import com.revaro.enums.SourceType;
import com.revaro.repository.TagRepository;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.CommentService;
import com.revaro.service.EventService;
import com.revaro.service.RsvpService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/events")
public class EventController {

    private static final String UPLOAD_FAILED = "Image upload failed. Try a different image.";

    private final EventService eventService;
    private final RsvpService rsvpService;
    private final CommentService commentService;
    private final TagRepository tagRepository;

    public EventController(EventService eventService,
                           RsvpService rsvpService,
                           CommentService commentService,
                           TagRepository tagRepository) {
        this.eventService = eventService;
        this.rsvpService = rsvpService;
        this.commentService = commentService;
        this.tagRepository = tagRepository;
    }

    @GetMapping("/{id}")
    public String eventDetail(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              Model model) {
        Event event = eventService.getById(id);
        model.addAttribute("event", event);
        model.addAttribute("comments", commentService.getCommentsForEvent(event));

        if (principal != null) {
            User user = principal.getUser();
            model.addAttribute("currentUser", user);
            model.addAttribute("likedCommentIds", commentService.getLikedCommentIds(user, event));
            rsvpService.getUserRsvpStatus(user, event)
                    .ifPresent(status -> model.addAttribute("userRsvpStatus", status));
        }
        return "event/detail";
    }

    @GetMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public String createForm(Model model) {
        model.addAttribute("eventDto", new EventDto());
        addFormData(model, "create");
        return "event/form";
    }

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public String createEvent(@Valid @ModelAttribute("eventDto") EventDto dto,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        requireOrganizerName(dto, bindingResult);
        if (bindingResult.hasErrors()) {
            addFormData(model, "create");
            return "event/form";
        }
        try {
            Event event = eventService.createEvent(dto, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Event posted!");
            return "redirect:/events/" + event.getId();
        } catch (IllegalArgumentException e) {
            return showFormError(model, "create", e.getMessage());
        } catch (IOException e) {
            return showFormError(model, "create", UPLOAD_FAILED);
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetailsImpl principal,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Event event = eventService.getById(id);
        if (!eventService.canEdit(event, principal.getUser())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to edit this event.");
            return "redirect:/events/" + id;
        }
        model.addAttribute("eventDto", eventService.toDto(event));
        model.addAttribute("event", event);
        addFormData(model, "edit");
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
        requireOrganizerName(dto, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("event", eventService.getById(id));
            addFormData(model, "edit");
            return "event/form";
        }
        try {
            eventService.updateEvent(id, dto, principal.getUser());
            int added = eventService.addDates(id, dto.getSpecificDates(), principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage",
                    added > 0 ? "Event updated and " + added + " more dates added." : "Event updated.");
            return "redirect:/events/" + id;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/events/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("event", eventService.getById(id));
            return showFormError(model, "edit", e.getMessage());
        } catch (IOException e) {
            model.addAttribute("event", eventService.getById(id));
            return showFormError(model, "edit", UPLOAD_FAILED);
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteEvent(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEvent(id, principal.getUser());
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/events/" + id;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Event deleted.");
        return "redirect:/my-events";
    }

    private void requireOrganizerName(EventDto dto, BindingResult bindingResult) {
        if (!dto.isPostedByOrganizer() && (dto.getOrganizerName() == null || dto.getOrganizerName().isBlank())) {
            bindingResult.rejectValue("organizerName", "required",
                    "Organizer name is required when posting for someone else.");
        }
    }

    private String showFormError(Model model, String formMode, String message) {
        model.addAttribute("errorMessage", message);
        addFormData(model, formMode);
        return "event/form";
    }

    private void addFormData(Model model, String formMode) {
        model.addAttribute("formMode", formMode);
        model.addAttribute("eventTypes", EventType.selectable());
        model.addAttribute("sourceTypes", SourceType.values());
        model.addAttribute("eventStatuses", EventStatus.values());
        model.addAttribute("allTags", tagRepository.findAllByOrderByCategoryAscNameAsc());
    }
}
