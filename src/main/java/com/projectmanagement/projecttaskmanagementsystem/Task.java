package com.projectmanagement.projecttaskmanagementsystem;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String status;
    private String priority;

    @Column(name = "task_role")
    private String taskRole;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "deadline")
    private LocalDate deadline;

    @ManyToOne
    @JoinColumn(name = "team_member_id")
    private TeamMember teamMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_admin_id")
    private AppUser ownerAdmin;

    public Task() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getTaskRole() { return taskRole; }
    public void setTaskRole(String taskRole) { this.taskRole = taskRole; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public TeamMember getTeamMember() { return teamMember; }
    public void setTeamMember(TeamMember teamMember) { this.teamMember = teamMember; }

    public AppUser getOwnerAdmin() { return ownerAdmin; }
    public void setOwnerAdmin(AppUser ownerAdmin) { this.ownerAdmin = ownerAdmin; }
}