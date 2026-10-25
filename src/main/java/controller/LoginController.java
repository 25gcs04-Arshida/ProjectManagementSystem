package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register-admin")
    public String showAdminRegistration() {
        return "register-admin";
    }

    @PostMapping("/register-admin")
    public String registerAdmin(
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        String cleanName = fullName == null ? "" : fullName.trim();
        String cleanUsername = username == null ? "" : username.trim();
        String cleanEmail = email == null
                ? ""
                : email.trim().toLowerCase(java.util.Locale.ROOT);

        if (cleanName.isBlank()
                || cleanUsername.isBlank()
                || cleanEmail.isBlank()
                || !cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || password == null
                || password.length() < 8) {

            model.addAttribute("error",
                    "Enter a valid name, username and email. "
                            + "Password must be at least 8 characters.");
            return "register-admin";
        }

        if (userRepository.existsByUsername(cleanUsername)) {
            model.addAttribute("error",
                    "That username is already taken.");
            return "register-admin";
        }

        AppUser user = new AppUser();
        user.setFullName(cleanName);
        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ADMIN");

        userRepository.save(user);

        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {

        if (authentication == null) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_ADMIN"));

        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_MANAGER"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        }

        if (isManager) {
            return "redirect:/manager/dashboard";
        }

        return "redirect:/employee/tasks";
    }
}