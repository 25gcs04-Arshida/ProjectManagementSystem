package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.Project;
import com.projectmanagement.projecttaskmanagementsystem.Task;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.ProjectRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TaskRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ManagerDashboardController {

    private final AppUserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ManagerDashboardController(
            AppUserRepository userRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @GetMapping("/manager/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        if (authentication == null) {
            throw new AccessDeniedException("Please log in.");
        }

        AppUser manager = userRepository.findByUsername(
                        authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Account not found."));

        if (!"MANAGER".equalsIgnoreCase(manager.getRole())) {
            throw new AccessDeniedException(
                    "Only managers can view this dashboard.");
        }

        Long adminId = manager.getWorkspaceAdminId();

        if (adminId == null) {
            throw new IllegalStateException(
                    "Your manager account has no admin workspace. "
                            + "Ask the administrator to create or repair your account.");
        }

        List<Project> projects =
                projectRepository.findByOwnerAdmin_Id(adminId);

        List<Task> tasks = taskRepository.findAll().stream()
                .filter(task -> {
                    if (task.getOwnerAdmin() != null
                            && adminId.equals(task.getOwnerAdmin().getId())) {
                        return true;
                    }

                    return task.getProject() != null
                            && task.getProject().getOwnerAdmin() != null
                            && adminId.equals(
                            task.getProject()
                                    .getOwnerAdmin().getId());
                })
                .toList();

        long completed = countStatus(tasks, "COMPLETED");
        long inProgress = countStatus(tasks, "IN PROGRESS");
        long pending = tasks.size() - completed - inProgress;

        model.addAttribute("manager", manager);
        model.addAttribute("projects", projects);
        model.addAttribute("tasks", tasks);
        model.addAttribute("projectCount", projects.size());
        model.addAttribute("taskCount", tasks.size());
        model.addAttribute("completedCount", completed);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("pendingCount", pending);

        return "manager-dashboard";
    }

    private long countStatus(List<Task> tasks, String target) {
        return tasks.stream()
                .filter(task -> task.getStatus() != null
                        && task.getStatus().trim()
                        .replace('_', ' ')
                        .equalsIgnoreCase(target))
                .count();
    }
}