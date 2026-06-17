package com.revaro.controller;

import com.revaro.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email,
                                 RedirectAttributes redirectAttributes) {
        passwordResetService.initiateReset(email.trim().toLowerCase());
        // Always show success — don't reveal if email exists
        redirectAttributes.addFlashAttribute("successMessage",
                "If that email is registered, you'll receive a reset link shortly. Check your inbox.");
        return "redirect:/auth/forgot-password";
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        if (passwordResetService.validateToken(token).isEmpty()) {
            model.addAttribute("errorMessage",
                    "This reset link is invalid or has expired. Please request a new one.");
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
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "auth/reset-password";
        }
        if (password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Password must be at least 8 characters.");
            return "auth/reset-password";
        }

        boolean success = passwordResetService.resetPassword(token, password);
        if (!success) {
            model.addAttribute("errorMessage",
                    "This reset link is invalid or has expired.");
            return "auth/reset-password";
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Password reset successfully! You can now log in.");
        return "redirect:/login";
    }
}
