package com.revaro.controller;

import com.revaro.repository.TagRepository;
import com.revaro.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final EventService eventService;
    private final TagRepository tagRepository;

    public HomeController(EventService eventService, TagRepository tagRepository) {
        this.eventService = eventService;
        this.tagRepository = tagRepository;
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
        return "index";
    }
}
