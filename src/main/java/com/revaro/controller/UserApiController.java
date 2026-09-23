package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.service.RevPointsService;
import com.revaro.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

// JSON endpoints used by the comment box: @mention autocomplete and the hover card on mentions
@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private static final int MENTION_SUGGESTIONS = 6;
    private static final DateTimeFormatter JOINED_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    private final UserService userService;
    private final RevPointsService revPointsService;

    public UserApiController(UserService userService, RevPointsService revPointsService) {
        this.userService = userService;
        this.revPointsService = revPointsService;
    }

    @GetMapping("/search")
    public List<Map<String, String>> search(@RequestParam String q) {
        return userService.searchByUsername(q, MENTION_SUGGESTIONS).stream()
                .map(user -> Map.of("username", user.getUsername(), "avatar", avatarOf(user)))
                .toList();
    }

    @GetMapping("/{username}/preview")
    public Map<String, Object> preview(@PathVariable String username) {
        User user = userService.getByUsername(username);
        return Map.of(
                "username", user.getUsername(),
                "avatar", avatarOf(user),
                "revPoints", revPointsService.allTimePoints().getOrDefault(user.getId(), 0L),
                "joined", user.getCreatedAt().format(JOINED_FORMAT));
    }

    private String avatarOf(User user) {
        return user.getProfileImage() != null ? user.getProfileImage() : "";
    }
}
