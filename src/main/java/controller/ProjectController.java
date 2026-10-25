package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.Project;
import com.projectmanagement.projecttaskmanagementsystem.Team;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.ProjectRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TeamRepository;

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
    private final TeamRepository teamRepository;

    public ProjectController(
            ProjectRepository projectRepository,
            AppUserRepository userRepository,
            TeamRepository teamRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    private AppUser getManager(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Please log in.");
        }

        AppUser user = userRepository.findByUsername(
                authentication.getName().trim()
        ).orElseThrow(() ->
                new AccessDeniedException("Manager account was not found.")
        );

        if (!"MANAGER".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException(
                    "Only managers can manage projects."
            );
        }

        if (user.getWorkspaceAdminId() == null) {
            throw new AccessDeniedException(
                    "This manager has no admin workspace."
            );
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
                projectRepository.findByOwnerAdmin_Id(adminId)
        );
        model.addAttribute(
                "teams",
                teamRepository.findByOwnerAdmin_Id(adminId)
        );

        return "projects";
    }

    @PostMapping("/projects/save")
    public String saveProject(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deadline,
            @RequestParam(required = false) Long teamId,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Project name cannot be empty."
            );
        }

        Project project = new Project();
        project.setName(name.trim());
        project.setDescription(description);
        project.setStatus(
                status == null || status.isBlank()
                        ? "Pending" : status.trim()
        );

        if (deadline != null && !deadline.isBlank()) {
            project.setDeadline(LocalDate.parse(deadline));
        }

        if (teamId != null) {
            Team team = teamRepository
                    .findByIdAndOwnerAdmin_Id(teamId, adminId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Select a team from your workspace."
                    ));
            project.setTeam(team);
        }

        AppUser admin = userRepository.findById(adminId)
                .orElseThrow(() ->
                        new IllegalStateException("Admin workspace not found.")
                );

        project.setOwnerAdmin(admin);
        projectRepository.save(project);

        return "redirect:/projects";
    }

    @GetMapping("/projects/delete/{id}")
    public String deleteProject(
            @PathVariable Long id,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Project project = projectRepository
                .findByIdAndOwnerAdmin_Id(
                        id, manager.getWorkspaceAdminId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found in your workspace."
                        )
                );

        projectRepository.delete(project);

        return "redirect:/projects";
    }
}