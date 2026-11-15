package com.devlab.taskboard.service;

import com.devlab.taskboard.dto.response.NotificationResponse;
import com.devlab.taskboard.dto.response.PageResponse;
import com.devlab.taskboard.dto.response.UnreadCountResponse;
import com.devlab.taskboard.entity.Notification;
import com.devlab.taskboard.entity.Task;
import com.devlab.taskboard.exception.ResourceNotFoundException;
import com.devlab.taskboard.mapper.NotificationMapper;
import com.devlab.taskboard.repository.NotificationRepository;
import com.devlab.taskboard.repository.TaskRepository;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    /** Client sort-u nəzərə alınmır: ən yeni bildiriş həmişə birinci. id -- səhifələr arası sabit sıra. */
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final NotificationMapper notificationMapper;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository,
                               TaskRepository taskRepository,
                               NotificationMapper notificationMapper,
                               Clock clock) {
        this.notificationRepository = notificationRepository;
        this.taskRepository = taskRepository;
        this.notificationMapper = notificationMapper;
        this.clock = clock;
    }

    public PageResponse<NotificationResponse> list(Long userId, Pageable pageable) {
        Pageable fixedSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), SORT);
        return PageResponse.from(
                notificationRepository.findAllByUserId(userId, fixedSort),
                notificationMapper::toResponse
        );
    }

    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public int createDueReminders(int batchSize) {
        List<Task> dueTasks = taskRepository.findDueReminders(Instant.now(clock), Limit.of(batchSize));

        List<Notification> notifications = new ArrayList<>(dueTasks.size());
        for (Task task : dueTasks) {
            notifications.add(new Notification(task.getOwner(), task, reminderMessage(task)));
            task.markReminderSent();
        }
        notificationRepository.saveAll(notifications);
        return notifications.size();
    }

    private static String reminderMessage(Task task) {
        String message = "Reminder: \"" + task.getTitle() + "\"";
        if (task.getDeadline() != null) {
            message += " is due at " + task.getDeadline();
        }
        return message;
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.markRead(Instant.now(clock));
        return notificationMapper.toResponse(notification);
    }
}
