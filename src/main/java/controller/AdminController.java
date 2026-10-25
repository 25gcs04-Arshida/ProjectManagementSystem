package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.TeamMember;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.ProjectRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TaskRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TeamMemberRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminController {

    private final AppUserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
            AppUserRepository userRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            TeamMemberRepository teamMemberRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Verify that the currently logged-in user is an administrator.
    private AppUser getLoggedInAdmin(Authentication authentication) {

        if (authentication == null) {
            throw new AccessDeniedException("Please log in.");
        }

        AppUser admin = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Admin account not found."));

        if (admin.getRole() == null
                || !admin.getRole().trim().equalsIgnoreCase("ADMIN")) {

            throw new AccessDeniedException(
                    "Only administrators can access this page.");
        }

        return admin;
    }

    // Display the administrator dashboard.
    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            Authentication authentication,
            Model model) {

        AppUser admin = getLoggedInAdmin(authentication);
        Long adminId = admin.getId();

        List<AppUser> users = userRepository.findAll().stream()
                .filter(user -> {
                    if (user.getId().equals(adminId)) {
                        return true;
                    }

                    AppUser owner = user.getOwnerAdmin();

                    return owner != null
                            && adminId.equals(owner.getId());
                })
                .toList();

        model.addAttribute("admin", admin);
        model.addAttribute("users", users);

        model.addAttribute(
                "projects",
                projectRepository.findByOwnerAdmin_Id(adminId));

        model.addAttribute(
                "tasks",
                taskRepository.findAll());

        model.addAttribute(
                "members",
                teamMemberRepository.findByOwnerAdmin_Id(adminId));

        return "admin-dashboard";
    }

    // Create a manager or employee account.
    @PostMapping("/admin/users/create")
    public String createUser(
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam(required = false) String email,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getLoggedInAdmin(authentication);

        String cleanName = fullName == null
                ? ""
                : fullName.trim();

        String cleanUsername = username == null
                ? ""
                : username.trim();

        String cleanEmail = email == null
                ? ""
                : email.trim();

        String cleanRole = role == null
                ? ""
                : role.trim().toUpperCase();

        // Validate the submitted information.
        if (cleanName.isBlank()
                || cleanUsername.isBlank()
                || password == null
                || password.length() < 8
                || (!cleanRole.equals("MANAGER")
                && !cleanRole.equals("EMPLOYEE"))) {

            model.addAttribute(
                    "error",
                    "Enter valid details. Password must be at least 8 characters.");

            return adminDashboard(authentication, model);
        }

        // Require an email address for each newly created account.
        if (cleanEmail.isBlank()
                || !cleanEmail.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

            model.addAttribute(
                    "error",
                    "Enter a valid email address.");

            return adminDashboard(authentication, model);
        }

        // Usernames must be unique.
        if (userRepository.existsByUsername(cleanUsername)) {

            model.addAttribute(
                    "error",
                    "Username already exists.");

            return adminDashboard(authentication, model);
        }

        // Create the login account.
        AppUser user = new AppUser();

        user.setFullName(cleanName);
        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(cleanRole);

        // Associate this account with the administrator who created it.
        user.setOwnerAdmin(admin);

        // Create a TeamMember record for employees.
        if (cleanRole.equals("EMPLOYEE")) {

            TeamMember member = new TeamMember();

            member.setName(cleanName);
            member.setEmail(cleanEmail);
            member.setRole("EMPLOYEE");
            member.setOwnerAdmin(admin);

            // Save the employee record before linking it to the login account.
            teamMemberRepository.save(member);

            user.setTeamMember(member);
        }

        // Save the login account with its email and assigned administrator.
        userRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "success",
                cleanRole + " account created successfully.");

        return "redirect:/admin/dashboard";
    }
}