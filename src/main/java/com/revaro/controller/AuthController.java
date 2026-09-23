package com.revaro.controller;

import com.revaro.dto.RegisterDto;
import com.revaro.security.UserDetailsImpl;
import com.revaro.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal UserDetailsImpl principal,
                            @RequestParam(required = false) String error,
                            @RequestParam(required = false) String expired,
                            Model model) {
        // Covers double submits too, the first click already signed them in
        if (principal != null) {
            return "redirect:/";
        }
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        } else if (expired != null) {
            model.addAttribute("infoMessage", "Your session expired, please sign in again.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDto") RegisterDto dto,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (!bindingResult.hasFieldErrors("confirmPassword") && !dto.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords don't match");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(dto.getUsername(), dto.getEmail(), dto.getPassword());
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Account created! Sign in to get started.");
        return "redirect:/login";
    }
}
