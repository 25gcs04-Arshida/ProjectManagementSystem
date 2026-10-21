package com.projectmanagement.projecttaskmanagementsystem.repository;

import com.projectmanagement.projecttaskmanagementsystem.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByTeamMember_Id(Long teamMemberId);

    List<Task> findByOwnerAdmin_Id(Long adminId);

    Optional<Task> findByIdAndOwnerAdmin_Id(Long id, Long adminId);

    List<Task> findByTeamMember_IdAndOwnerAdmin_Id(
            Long teamMemberId, Long adminId);

    long countByStatus(String status);

    long countByOwnerAdmin_IdAndStatus(Long adminId, String status);
}