package com.projectmanagement.projecttaskmanagementsystem;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_admin_id", nullable = false)
    private AppUser ownerAdmin;

    @ManyToMany
    @JoinTable(
            name = "project_team_members",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "team_member_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"team_id", "team_member_id"}
            )
    )
    private List<TeamMember> members = new ArrayList<>();

    public Team() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AppUser getOwnerAdmin() {
        return ownerAdmin;
    }

    public void setOwnerAdmin(AppUser ownerAdmin) {
        this.ownerAdmin = ownerAdmin;
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }
}