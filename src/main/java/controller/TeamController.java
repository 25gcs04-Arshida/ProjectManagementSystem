package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.Team;
import com.projectmanagement.projecttaskmanagementsystem.TeamMember;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TeamMemberRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TeamRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TeamController {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AppUserRepository userRepository;

    public TeamController(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            AppUserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    private AppUser getManager(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Please log in.");
        }

        AppUser manager = userRepository.findByUsername(
                authentication.getName().trim()
        ).orElseThrow(() ->
                new AccessDeniedException("User not found.")
        );

        if (!"MANAGER".equalsIgnoreCase(manager.getRole())) {
            throw new AccessDeniedException(
                    "Only managers can create teams."
            );
        }

        if (manager.getWorkspaceAdminId() == null) {
            throw new AccessDeniedException(
                    "Your account has no admin workspace."
            );
        }

        return manager;
    }

    @GetMapping("/teams")
    public String showTeams(
            Model model,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        model.addAttribute("teams", teamRepository.findByOwnerAdmin_Id(adminId));
        model.addAttribute(
                "employees",
                teamMemberRepository.findByOwnerAdmin_Id(adminId)
        );

        return "teams";
    }

    @PostMapping("/teams/save")
    public String saveTeam(
            @RequestParam String name,
            @RequestParam(required = false) List<Long> memberIds,
            Authentication authentication) {

        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name is required.");
        }

        if (memberIds == null || memberIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one existing employee."
            );
        }

        List<TeamMember> selectedMembers = new ArrayList<>();

        for (Long memberId : memberIds) {
            TeamMember member = teamMemberRepository
                    .findByIdAndOwnerAdmin_Id(memberId, adminId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "One or more selected employees are outside your workspace."
                    ));

            if (selectedMembers.stream().noneMatch(
                    existing -> existing.getId().equals(member.getId()))) {
                selectedMembers.add(member);
            }
        }

        AppUser admin = userRepository.findById(adminId)
                .orElseThrow(() ->
                        new IllegalStateException("Admin workspace not found.")
                );

        Team team = new Team();
        team.setName(name.trim());
        team.setOwnerAdmin(admin);
        team.setMembers(selectedMembers);

        teamRepository.save(team);

        return "redirect:/teams";
    }

    @GetMapping("/teams/delete/{id}")
    public String deleteTeam(
            @PathVariable Long id,
            Authentication authentication) {

        AppUser manager = getManager(authentication);

        Team team = teamRepository
                .findByIdAndOwnerAdmin_Id(
                        id, manager.getWorkspaceAdminId()
                )
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Team not found in your workspace."
                        )
                );

        teamRepository.delete(team);

        return "redirect:/teams";
    }
}