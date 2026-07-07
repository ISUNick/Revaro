package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.repository.TagRepository;
import com.revaro.repository.UserRepository;
import com.revaro.service.EventService;
import com.revaro.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            @RequestParam(required = false) String organizer,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            Model model) {

        model.addAttribute("events",
                eventService.findEvents(q, state, type, tag, organizer, sort, page));
        model.addAttribute("allTags",
                tagRepository.findAllByOrderByCategoryAscNameAsc());
        model.addAttribute("currentPage", "home");

        // Show user cards only when query closely matches a username
        if (q != null && q.length() >= 2) {
            List<User> matchedUsers = userRepository
                    .findByUsernameContainingIgnoreCase(q)
                    .stream().limit(3).toList();

            if (!matchedUsers.isEmpty()) {
                model.addAttribute("searchUsers", matchedUsers);
                Map<Long, Long> userRevPoints = new HashMap<>();
                for (User u : matchedUsers) {
                    userRevPoints.put(u.getId(), userService.calculateRevPoints(u));
                }
                model.addAttribute("userRevPoints", userRevPoints);
            }
        }

        return "index";
    }
}
