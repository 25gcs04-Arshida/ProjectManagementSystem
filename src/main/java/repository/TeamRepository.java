package com.projectmanagement.projecttaskmanagementsystem.repository;

import com.projectmanagement.projecttaskmanagementsystem.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByOwnerAdmin_Id(Long adminId);

    Optional<Team> findByIdAndOwnerAdmin_Id(
            Long id, Long adminId
    );
}