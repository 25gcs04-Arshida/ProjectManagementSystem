package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.Task;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TaskRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/employee/tasks")
public class EmployeeTaskController {

    private final AppUserRepository userRepository;
    private final TaskRepository taskRepository;

    public EmployeeTaskController(
            AppUserRepository userRepository,
            TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public String viewMyTasks(
            Authentication authentication,
            Model model) {

        AppUser user = getLoggedInUser(authentication);

        if (!"EMPLOYEE".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Employee access required.");
        }

        model.addAttribute("employee", user);

        if (user.getTeamMember() == null) {
            addEmptyDashboard(model);
            model.addAttribute("error",
                    "Your account is not linked to an employee profile. Contact your administrator.");
            return "employee-dashboard";
        }

        Long adminId = user.getWorkspaceAdminId();

        if (adminId == null) {
            addEmptyDashboard(model);
            model.addAttribute("error",
                    "Your account has no admin workspace. Contact your administrator.");
            return "employee-dashboard";
        }

        List<Task> tasks =
                taskRepository.findByTeamMember_IdAndOwnerAdmin_Id(
                        user.getTeamMember().getId(), adminId);

        long completedCount = countStatus(tasks, "Completed");
        long inProgressCount = countStatus(tasks, "In Progress");
        long pendingCount = countStatus(tasks, "Pending");

        model.addAttribute("tasks", tasks);
        model.addAttribute("totalTasks", tasks.size());
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("pendingCount", pendingCount);

        return "employee-dashboard";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {

        AppUser user = getLoggedInUser(authentication);

        if (!"EMPLOYEE".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Employee access required.");
        }

        if (user.getTeamMember() == null) {
            throw new AccessDeniedException(
                    "Your account has no employee profile.");
        }

        String normalizedStatus = status == null
                ? ""
                : status.trim().replace('_', ' ');

        if (!List.of("Pending", "In Progress", "Completed")
                .stream()
                .anyMatch(s -> s.equalsIgnoreCase(normalizedStatus))) {
            throw new IllegalArgumentException("Invalid task status.");
        }

        Long adminId = user.getWorkspaceAdminId();

        if (adminId == null) {
            throw new AccessDeniedException(
                    "Your account has no admin workspace.");
        }

        Task task = taskRepository
                .findByIdAndOwnerAdmin_Id(id, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Task not found."));

        if (task.getTeamMember() == null
                || !task.getTeamMember().getId().equals(
                user.getTeamMember().getId())) {
            throw new AccessDeniedException(
                    "You cannot update another employee's task.");
        }

        task.setStatus(normalizedStatus);
        taskRepository.save(task);

        return "redirect:/employee/tasks";
    }

    private long countStatus(List<Task> tasks, String target) {
        return tasks.stream()
                .filter(task -> task.getStatus() != null
                        && task.getStatus().trim()
                        .replace('_', ' ')
                        .equalsIgnoreCase(target))
                .count();
    }

    private void addEmptyDashboard(Model model) {
        model.addAttribute("tasks", List.of());
        model.addAttribute("totalTasks", 0);
        model.addAttribute("completedCount", 0);
        model.addAttribute("inProgressCount", 0);
        model.addAttribute("pendingCount", 0);
    }

    private AppUser getLoggedInUser(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Please log in.");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));
    }
}