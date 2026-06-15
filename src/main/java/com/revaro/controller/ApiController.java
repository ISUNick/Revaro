package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final UserService userService;

    public ApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile-image")
    public ResponseEntity<?> profileImage(@AuthenticationPrincipal UserDetailsImpl principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of());
        }
        User user = userService.findById(principal.getUser().getId()).orElse(null);
        if (user == null || user.getProfileImage() == null) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(Map.of("profileImage", user.getProfileImage()));
    }
}
