package com.devlab.taskboard.repository;

import com.devlab.taskboard.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUserId(Long userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Notification n where n.task.id = :taskId")
    int deleteAllByTaskId(@Param("taskId") Long taskId);
}
