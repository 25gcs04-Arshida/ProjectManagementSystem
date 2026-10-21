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

    private AppUser getLoggedInAdmin(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Please log in.");
        }

        AppUser admin = userRepository.findByUsername(
                        authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Admin account not found."));

        if (admin.getRole() == null
                || !admin.getRole().trim().equalsIgnoreCase("ADMIN")) {
            throw new AccessDeniedException(
                    "Only administrators can access this page.");
        }

        return admin;
    }

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
        model.addAttribute("projects",
                projectRepository.findByOwnerAdmin_Id(adminId));
        model.addAttribute("tasks",
                taskRepository.findAll());
        model.addAttribute("members",
                teamMemberRepository.findByOwnerAdmin_Id(adminId));

        return "admin-dashboard";
    }

    @PostMapping("/admin/users/create")
    public String createUser(
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam(required = false) String email,
            Authentication authentication,
            Model model) {

        AppUser admin = getLoggedInAdmin(authentication);

        String cleanName = fullName == null ? "" : fullName.trim();
        String cleanUsername = username == null ? "" : username.trim();
        String cleanRole = role == null ? "" : role.trim().toUpperCase();

        if (cleanName.isBlank()
                || cleanUsername.isBlank()
                || password == null
                || password.length() < 8
                || (!cleanRole.equals("MANAGER")
                && !cleanRole.equals("EMPLOYEE"))) {

            model.addAttribute("error",
                    "Enter valid details. Password must be at least 8 characters.");
            return adminDashboard(authentication, model);
        }

        if (userRepository.existsByUsername(cleanUsername)) {
            model.addAttribute("error", "Username already exists.");
            return adminDashboard(authentication, model);
        }

        AppUser user = new AppUser();
        user.setFullName(cleanName);
        user.setUsername(cleanUsername);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(cleanRole);

        // Link the new login account to this administrator.
        user.setOwnerAdmin(admin);

        if (cleanRole.equals("EMPLOYEE")) {
            TeamMember member = new TeamMember();
            member.setName(cleanName);
            member.setEmail(email == null ? "" : email.trim());
            member.setRole("EMPLOYEE");
            member.setOwnerAdmin(admin);

            teamMemberRepository.save(member);
            user.setTeamMember(member);
        }

        userRepository.save(user);

        return "redirect:/admin/dashboard";
    }
}