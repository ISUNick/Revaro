package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.repository.UserRepository;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class LeaderboardController {

    private final UserRepository userRepository;
    private final UserService userService;

    public LeaderboardController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {

        List<User> allUsers = userRepository.findAll();

        // Calculate points for every user
        Map<Long, Long> pointsMap = new HashMap<>();
        for (User u : allUsers) {
            pointsMap.put(u.getId(), userService.calculateRevPoints(u));
        }

        // All-time: top 10 sorted by total points
        List<User> allTime = allUsers.stream()
                .filter(u -> pointsMap.getOrDefault(u.getId(), 0L) > 0)
                .sorted(Comparator.comparingLong(
                        (User u) -> pointsMap.getOrDefault(u.getId(), 0L)).reversed())
                .limit(10)
                .toList();

        // Week: same for now (we don't track weekly points separately yet,
        // so we show all-time top 5 in the "this week" slot and label it clearly)
        List<User> thisWeek = allTime.stream().limit(5).toList();

        // Top organizer: most events created
        List<User> topOrganizers = allUsers.stream()
                .sorted(Comparator.comparingLong(
                        (User u) -> userService.countEventsForUser(u)).reversed())
                .filter(u -> userService.countEventsForUser(u) > 0)
                .limit(5)
                .toList();

        // Current user rank
        Long currentUserRank = null;
        Long currentUserPoints = null;
        if (principal != null) {
            User currentUser = principal.getUser();
            final long pts = userService.calculateRevPoints(currentUser);
            currentUserPoints = pts;
            long rank = allUsers.stream()
                    .filter(u -> pointsMap.getOrDefault(u.getId(), 0L) > pts)
                    .count() + 1;
            currentUserRank = rank;
        }

        model.addAttribute("allTime", allTime);
        model.addAttribute("thisWeek", thisWeek);
        model.addAttribute("topOrganizers", topOrganizers);
        model.addAttribute("pointsMap", pointsMap);
        model.addAttribute("currentUserRank", currentUserRank);
        model.addAttribute("currentUserPoints", currentUserPoints);

        return "leaderboard";
    }
}
