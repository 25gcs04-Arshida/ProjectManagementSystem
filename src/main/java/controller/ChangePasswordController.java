package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChangePasswordController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/change-password")
    public String showChangePasswordPage(
            Authentication authentication,
            Model model) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            Authentication authentication,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Logged-in user account was not found."));

        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Please fill in all password fields.");

            return "redirect:/change-password";
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Your current password is incorrect.");

            return "redirect:/change-password";
        }

        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "The new password must contain at least 8 characters.");

            return "redirect:/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "The new password and confirmation do not match.");

            return "redirect:/change-password";
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Your new password must be different from your current password.");

            return "redirect:/change-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "successMessage", "Your password has been changed successfully.");

        return "redirect:/change-password";
    }

}
