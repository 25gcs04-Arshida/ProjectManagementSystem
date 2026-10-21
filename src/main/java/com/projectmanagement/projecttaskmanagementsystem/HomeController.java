package com.projectmanagement.projecttaskmanagementsystem;

import com.projectmanagement.projecttaskmanagementsystem.repository.*;
import com.projectmanagement.projecttaskmanagementsystem.repository.TeamMemberRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;

    public HomeController(ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          TeamMemberRepository teamMemberRepository) {

        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @GetMapping("/")
    public String home(Model model) {

        long totalProjects = projectRepository.count();

        long totalTasks = taskRepository.count();

        long completedTasks = taskRepository.countByStatus("Completed");

        long notStartedTasks = taskRepository.countByStatus("Not Started");

        long inProgressTasks = taskRepository.countByStatus("In Progress");

        long teamMembers = teamMemberRepository.count();

        long completedProjects =
                projectRepository.countByStatus("Completed");

        long notStartedProjects =
                projectRepository.countByStatus("Not Started");

        long inProgressProjects =
                projectRepository.countByStatus("In Progress");

        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("notStartedTasks", notStartedTasks);
        model.addAttribute("inProgressTasks", inProgressTasks);
        model.addAttribute("teamMembers", teamMembers);

        model.addAttribute("completedProjects", completedProjects);
        model.addAttribute("notStartedProjects", notStartedProjects);
        model.addAttribute("inProgressProjects", inProgressProjects);

        return "index";
    }
}