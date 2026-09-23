package com.revaro.controller;

import com.revaro.security.UserDetailsImpl;
import com.revaro.service.RevPointsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LeaderboardController {

    private static final int MAIN_LIST_SIZE = 10;
    private static final int SIDE_LIST_SIZE = 5;

    private final RevPointsService revPointsService;

    public LeaderboardController(RevPointsService revPointsService) {
        this.revPointsService = revPointsService;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        model.addAttribute("allTime", revPointsService.topAllTime(MAIN_LIST_SIZE));
        model.addAttribute("thisWeek", revPointsService.topThisWeek(SIDE_LIST_SIZE));
        model.addAttribute("topOrganizers", revPointsService.topOrganizers(SIDE_LIST_SIZE));
        if (principal != null) {
            model.addAttribute("standing", revPointsService.standingOf(principal.getUser()));
        }
        return "leaderboard";
    }
}
