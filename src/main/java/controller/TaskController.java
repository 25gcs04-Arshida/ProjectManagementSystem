package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.Project;
import com.projectmanagement.projecttaskmanagementsystem.Task;
import com.projectmanagement.projecttaskmanagementsystem.TeamMember;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.ProjectRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TaskRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TeamMemberRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
public class TaskController {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AppUserRepository userRepository;

    public TaskController(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            TeamMemberRepository teamMemberRepository,
            AppUserRepository userRepository) {

        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    private AppUser getManager(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Please log in.");
        }

        AppUser user = userRepository.findByUsername(
                authentication.getName()
        ).orElseThrow(() ->
                new AccessDeniedException("User not found.")
        );

        if (!"MANAGER".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException(
                    "Only managers can manage tasks."
            );
        }

        if (user.getWorkspaceAdminId() == null) {
            throw new AccessDeniedException(
                    "This manager has no admin workspace."
            );
        }

        return user;
    }

    private void loadFormData(Model model, Long adminId) {
        model.addAttribute(
                "projects",
                projectRepository.findByOwnerAdmin_Id(adminId)
        );

        model.addAttribute(
                "teamMembers",
                teamMemberRepository.findByOwnerAdmin_Id(adminId)
        );
    }

    @GetMapping("/tasks")
    public String showTasks(
            @RequestParam(required = false) Long projectId,
            Model model,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        model.addAttribute("task", new Task());
        model.addAttribute(
                "tasks",
                taskRepository.findByOwnerAdmin_Id(adminId)
        );
        model.addAttribute("selectedProjectId", projectId);

        loadFormData(model, adminId);

        return "tasks";
    }

    @PostMapping("/tasks/save")
    public String saveTask(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String status,
            @RequestParam String priority,
            @RequestParam Long projectId,
            @RequestParam Long teamMemberId,
            @RequestParam(required = false) String deadline,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        Project project = projectRepository
                .findByIdAndOwnerAdmin_Id(projectId, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Select a project from your workspace."
                        )
                );

        TeamMember member = teamMemberRepository
                .findByIdAndOwnerAdmin_Id(teamMemberId, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Select a team member from your workspace."
                        )
                );

        AppUser admin = userRepository.findById(adminId)
                .orElseThrow(() ->
                        new IllegalStateException("Admin not found.")
                );

        Task task = new Task();
        task.setTitle(title.trim());
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setProject(project);
        task.setTeamMember(member);
        task.setOwnerAdmin(admin);

        if (deadline != null && !deadline.isBlank()) {
            task.setDeadline(LocalDate.parse(deadline));
        }

        taskRepository.save(task);

        return "redirect:/tasks?projectId=" + projectId;
    }

    @GetMapping("/tasks/edit/{id}")
    public String editTask(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        Task task = taskRepository.findByIdAndOwnerAdmin_Id(id, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Task not found.")
                );

        model.addAttribute("task", task);
        loadFormData(model, adminId);

        return "edit-task";
    }

    @PostMapping("/tasks/update")
    public String updateTask(
            @RequestParam Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String status,
            @RequestParam String priority,
            @RequestParam Long projectId,
            @RequestParam Long teamMemberId,
            @RequestParam(required = false) String deadline,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        Task task = taskRepository.findByIdAndOwnerAdmin_Id(id, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Task not found.")
                );

        Project project = projectRepository
                .findByIdAndOwnerAdmin_Id(projectId, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Project not found.")
                );

        TeamMember member = teamMemberRepository
                .findByIdAndOwnerAdmin_Id(teamMemberId, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Team member not found.")
                );

        task.setTitle(title.trim());
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setProject(project);
        task.setTeamMember(member);
        task.setDeadline(
                deadline == null || deadline.isBlank()
                        ? null
                        : LocalDate.parse(deadline)
        );

        taskRepository.save(task);

        return "redirect:/tasks?projectId=" + projectId;
    }

    @GetMapping("/tasks/delete/{id}")
    public String deleteTask(
            @PathVariable Long id,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        Task task = taskRepository.findByIdAndOwnerAdmin_Id(id, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Task not found.")
                );

        Long projectId = task.getProject() == null
                ? null
                : task.getProject().getId();

        taskRepository.delete(task);

        return projectId == null
                ? "redirect:/tasks"
                : "redirect:/tasks?projectId=" + projectId;
    }
}