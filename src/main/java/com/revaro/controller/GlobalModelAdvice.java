package com.revaro.controller;

import com.revaro.entity.User;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds currentNavUser to every page's model automatically.
 * Used to show the profile picture in the navbar without JS.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;

    public GlobalModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentNavUser")
    public User currentNavUser(@AuthenticationPrincipal UserDetailsImpl principal) {
        if (principal == null) return null;
        return userService.findById(principal.getUser().getId()).orElse(null);
    }
}
