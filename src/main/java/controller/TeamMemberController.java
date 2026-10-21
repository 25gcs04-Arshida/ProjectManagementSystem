package com.projectmanagement.projecttaskmanagementsystem.controller;

import com.projectmanagement.projecttaskmanagementsystem.AppUser;
import com.projectmanagement.projecttaskmanagementsystem.TeamMember;
import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import com.projectmanagement.projecttaskmanagementsystem.repository.TeamMemberRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class TeamMemberController {

    private final TeamMemberRepository teamMemberRepository;
    private final AppUserRepository userRepository;

    public TeamMemberController(
            TeamMemberRepository teamMemberRepository,
            AppUserRepository userRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    private AppUser getManager(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Please log in.");
        }

        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("User not found."));

        if (!"MANAGER".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException(
                    "Only managers can manage team members.");
        }

        if (user.getWorkspaceAdminId() == null) {
            throw new IllegalStateException(
                    "Your account has no admin workspace.");
        }

        return user;
    }

    @GetMapping("/team")
    public String team(Authentication authentication, Model model) {
        AppUser manager = getManager(authentication);
        Long adminId = manager.getWorkspaceAdminId();

        model.addAttribute("member", new TeamMember());
        model.addAttribute("members",
                teamMemberRepository.findByOwnerAdmin_Id(adminId));

        return "team";
    }

    @PostMapping("/team/save")
    public String saveMember(
            @ModelAttribute("member") TeamMember member,
            Authentication authentication) {

        AppUser manager = getManager(authentication);

        if (member.getName() == null || member.getName().isBlank()) {
            throw new IllegalArgumentException("Member name is required.");
        }

        if (member.getEmail() == null || member.getEmail().isBlank()) {
            throw new IllegalArgumentException("Member email is required.");
        }

        AppUser admin = userRepository.findById(
                        manager.getWorkspaceAdminId())
                .orElseThrow(() ->
                        new IllegalStateException("Admin workspace not found."));

        member.setName(member.getName().trim());
        member.setEmail(member.getEmail().trim());
        member.setOwnerAdmin(admin);

        teamMemberRepository.save(member);
        return "redirect:/team";
    }

    @GetMapping("/team/edit/{id}")
    public String editMember(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        AppUser manager = getManager(authentication);

        TeamMember member = teamMemberRepository
                .findByIdAndOwnerAdmin_Id(
                        id, manager.getWorkspaceAdminId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Member not found in your workspace."));

        model.addAttribute("teamMember", member);
        return "edit-team-member";
    }

    @PostMapping("/team/update")
    public String updateMember(
            @ModelAttribute("teamMember") TeamMember submitted,
            Authentication authentication) {

        AppUser manager = getManager(authentication);

        TeamMember existing = teamMemberRepository
                .findByIdAndOwnerAdmin_Id(
                        submitted.getId(), manager.getWorkspaceAdminId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Member not found in your workspace."));

        existing.setName(submitted.getName().trim());
        existing.setEmail(submitted.getEmail().trim());
        existing.setRole(submitted.getRole());

        teamMemberRepository.save(existing);
        return "redirect:/team";
    }

    @GetMapping("/team/delete/{id}")
    public String deleteMember(
            @PathVariable Long id,
            Authentication authentication) {

        AppUser manager = getManager(authentication);

        TeamMember member = teamMemberRepository
                .findByIdAndOwnerAdmin_Id(
                        id, manager.getWorkspaceAdminId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Member not found in your workspace."));

        teamMemberRepository.delete(member);
        return "redirect:/team";
    }
}