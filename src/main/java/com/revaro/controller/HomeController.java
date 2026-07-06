package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.repository.TagRepository;
import com.revaro.repository.UserRepository;
import com.revaro.service.UserService;
import com.revaro.service.EventService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Controller
public class HomeController {

    private final EventService eventService;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public HomeController(EventService eventService,
                          TagRepository tagRepository,
                          UserRepository userRepository,
                          UserService userService) {
        this.eventService = eventService;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            Model model) {

        model.addAttribute("events", eventService.findEvents(q, state, type, tag, sort, page));
        model.addAttribute("allTags", tagRepository.findAllByOrderByCategoryAscNameAsc());
        model.addAttribute("currentPage", "home");

        // Search users when there's a query — show 3 user cards
        if (q != null && !q.isBlank()) {
            List<User> matchedUsers = userRepository.findByUsernameContainingIgnoreCase(q);

            // Fill to 3 using top rev point users if needed
            LinkedHashSet<User> userRow = new LinkedHashSet<>(matchedUsers);
            if (userRow.size() < 3) {
                List<User> topUsers = userRepository.findTopByEventCount(PageRequest.of(0, 10));
                for (User u : topUsers) {
                    if (userRow.size() >= 3) break;
                    userRow.add(u);
                }
            }
            List<User> userList = new ArrayList<>(userRow).subList(0, Math.min(3, userRow.size()));
            model.addAttribute("searchUsers", userList);

            // Pass rev points for each user
            java.util.Map<Long, Long> userRevPoints = new java.util.HashMap<>();
            for (User u : userList) {
                userRevPoints.put(u.getId(), userService.calculateRevPoints(u));
            }
            model.addAttribute("userRevPoints", userRevPoints);
        }

        return "index";
    }
}
