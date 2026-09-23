package com.revaro.controller;

import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.EventService;
import com.revaro.service.RevPointsService;
import com.revaro.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
public class ProfileController {

    private final UserService userService;
    private final EventService eventService;
    private final RevPointsService revPointsService;

    public ProfileController(UserService userService,
                             EventService eventService,
                             RevPointsService revPointsService) {
        this.userService = userService;
        this.eventService = eventService;
        this.revPointsService = revPointsService;
    }

    @GetMapping("/profile/{username}")
    public String profile(@PathVariable String username, Model model) {
        User user = userService.getByUsername(username);
        List<Event> events = eventService.getEventsByCreator(user);
        RevPointsService.Standing standing = revPointsService.standingOf(user);

        model.addAttribute("profileUser", user);
        model.addAttribute("profileEvents", events);
        model.addAttribute("eventCount", events.size());
        model.addAttribute("revPoints", standing.points());
        model.addAttribute("userRank", standing.rank());
        return "user/profile";
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String ownProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        return "redirect:/profile/" + principal.getUsername();
    }

    @GetMapping("/profile/edit")
    @PreAuthorize("isAuthenticated()")
    public String editProfile(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        model.addAttribute("user", userService.getById(principal.getId()));
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
            userService.updateProfile(principal.getId(), profileImageFile, carYear, carMake, carModel, carImageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Image upload failed. Try a different image.");
        }
        return "redirect:/profile/" + principal.getUsername();
    }

    @GetMapping("/my-events")
    @PreAuthorize("isAuthenticated()")
    public String myEvents(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        model.addAttribute("events", eventService.getEventsByCreator(principal.getUser()));
        return "user/my-events";
    }
}
