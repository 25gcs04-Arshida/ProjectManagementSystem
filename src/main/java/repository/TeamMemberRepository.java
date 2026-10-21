package com.projectmanagement.projecttaskmanagementsystem.repository;

import com.projectmanagement.projecttaskmanagementsystem.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository
        extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByOwnerAdmin_Id(Long adminId);

    Optional<TeamMember> findByIdAndOwnerAdmin_Id(
            Long id, Long adminId);
}