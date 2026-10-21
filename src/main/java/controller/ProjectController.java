package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.Project;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.ProjectRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final AppUserRepository userRepository;

    public ProjectController(
            ProjectRepository projectRepository,
            AppUserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    private AppUser getManager(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Please log in.");
        }

        AppUser user = userRepository.findByUsername(
                        authentication.getName().trim())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Manager account was not found."));

        if (user.getRole() == null ||
                !user.getRole().trim().equalsIgnoreCase("MANAGER")) {
            throw new AccessDeniedException(
                    "Only managers can manage projects.");
        }

        if (user.getWorkspaceAdminId() == null) {
            throw new IllegalStateException(
                    "This manager is not linked to an admin workspace. "
                            + "Create a new manager account from the admin dashboard.");
        }

        return user;
    }

    @GetMapping("/projects")
    public String showProjects(
            Model model,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        model.addAttribute("project", new Project());
        model.addAttribute(
                "projects",
                projectRepository.findByOwnerAdmin_Id(adminId));

        return "projects";
    }

    @PostMapping("/projects/save")
    public String saveProject(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deadline,
            Authentication authentication) {

        AppUser manager = getManager(authentication);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Project name cannot be empty.");
        }

        Project project = new Project();
        project.setName(name.trim());
        project.setDescription(description);
        project.setStatus(
                status == null || status.isBlank()
                        ? "Pending" : status.trim());

        if (deadline != null && !deadline.isBlank()) {
            project.setDeadline(LocalDate.parse(deadline));
        }

        AppUser admin = userRepository.findById(
                        manager.getWorkspaceAdminId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Admin workspace was not found."));

        project.setOwnerAdmin(admin);
        projectRepository.save(project);

        return "redirect:/projects";
    }

    @GetMapping("/projects/delete/{id}")
    public String deleteProject(
            @PathVariable Long id,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        Project project = projectRepository
                .findByIdAndOwnerAdmin_Id(id, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found in your workspace."));

        projectRepository.delete(project);

        return "redirect:/projects";
    }
}