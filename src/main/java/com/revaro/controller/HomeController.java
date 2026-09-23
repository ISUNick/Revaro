package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.service.EventService;
import com.revaro.service.RevPointsService;
import com.revaro.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private static final int ACCOUNT_PREVIEW_LIMIT = 3;
    private static final int ACCOUNT_SEARCH_LIMIT = 12;

    private final EventService eventService;
    private final UserService userService;
    private final RevPointsService revPointsService;

    public HomeController(EventService eventService, UserService userService, RevPointsService revPointsService) {
        this.eventService = eventService;
        this.userService = userService;
        this.revPointsService = revPointsService;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String searchIn,
                       @RequestParam(defaultValue = "relevance") String sort,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("q", q);
        model.addAttribute("searchIn", searchIn == null ? "" : searchIn);
        model.addAttribute("sort", sort);

        boolean accountsOnly = "accounts".equals(searchIn);
        if (!accountsOnly) {
            model.addAttribute("events", eventService.findEvents(q, searchIn, sort, page));
        }

        // Matching accounts show above the events, or on their own with the Accounts filter
        if (q != null && q.trim().length() >= 2) {
            List<User> users = userService.searchByUsername(q,
                    accountsOnly ? ACCOUNT_SEARCH_LIMIT : ACCOUNT_PREVIEW_LIMIT);
            model.addAttribute("searchUsers", users);
            if (!users.isEmpty()) {
                model.addAttribute("userRevPoints", revPointsService.allTimePoints());
            }
        }
        return "index";
    }
}
