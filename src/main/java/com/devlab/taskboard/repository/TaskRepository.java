package com.devlab.taskboard.repository;

import com.devlab.taskboard.entity.Task;
import com.devlab.taskboard.enums.TaskStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = "labels")
    List<Task> findAllByOwnerIdAndStatusOrderByCompletedAtDesc(Long ownerId, TaskStatus status);

    @Query("""
            select t from Task t
            where t.reminderAt <= :now
              and t.reminderSent = false
              and t.status <> com.devlab.taskboard.enums.TaskStatus.DONE
            order by t.reminderAt asc
            """)
    List<Task> findDueReminders(@Param("now") Instant now, Limit limit);
}
