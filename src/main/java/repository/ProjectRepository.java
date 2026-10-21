package com.projectmanagement.projecttaskmanagementsystem.repository;

import com.projectmanagement.projecttaskmanagementsystem.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerAdmin_Id(Long adminId);

    Optional<Project> findByIdAndOwnerAdmin_Id(
            Long id, Long adminId);

    long countByStatus(String status);

    long countByOwnerAdmin_IdAndStatus(Long adminId, String status);
}