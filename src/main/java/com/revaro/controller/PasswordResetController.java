package com.revaro.controller;

import com.revaro.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class PasswordResetController {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String INVALID_LINK = "This reset link is invalid or has expired. Request a new one below.";

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        passwordResetService.requestReset(email);
        // Same message either way so this can't be used to check if an email has an account
        redirectAttributes.addFlashAttribute("successMessage",
                "If that email is registered, a reset link is on its way. Check your inbox.");
        return "redirect:/auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        if (!passwordResetService.isValidToken(token)) {
            model.addAttribute("errorMessage", INVALID_LINK);
            return "auth/forgot-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        String error = null;
        if (!password.equals(confirmPassword)) {
            error = "Passwords don't match.";
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            error = "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        if (error != null) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", error);
            return "auth/reset-password";
        }

        if (!passwordResetService.resetPassword(token, password)) {
            model.addAttribute("errorMessage", INVALID_LINK);
            return "auth/forgot-password";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Password reset! You can sign in now.");
        return "redirect:/login";
    }
}
