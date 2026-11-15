package com.devlab.taskboard.service;

import com.devlab.taskboard.dto.request.TaskCreateRequest;
import com.devlab.taskboard.dto.request.TaskFilter;
import com.devlab.taskboard.dto.request.TaskStatusUpdateRequest;
import com.devlab.taskboard.dto.request.TaskUpdateRequest;
import com.devlab.taskboard.dto.response.PageResponse;
import com.devlab.taskboard.dto.response.TaskResponse;
import com.devlab.taskboard.entity.Label;
import com.devlab.taskboard.entity.Task;
import com.devlab.taskboard.entity.User;
import com.devlab.taskboard.enums.TaskStatus;
import com.devlab.taskboard.exception.BadRequestException;
import com.devlab.taskboard.exception.InvalidStatusTransitionException;
import com.devlab.taskboard.exception.ResourceNotFoundException;
import com.devlab.taskboard.mapper.TaskMapper;
import com.devlab.taskboard.repository.LabelRepository;
import com.devlab.taskboard.repository.NotificationRepository;
import com.devlab.taskboard.repository.TaskRepository;
import com.devlab.taskboard.repository.UserRepository;
import com.devlab.taskboard.specification.TaskSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("title", "status", "deadline", "createdAt", "updatedAt", "completedAt");

    private static final String ALLOWED_SORT_FIELDS_TEXT =
            String.join(", ", ALLOWED_SORT_FIELDS.stream().sorted().toList());

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private static final Sort TIE_BREAKER = Sort.by(Sort.Direction.DESC, "id");

    private final TaskRepository taskRepository;
    private final LabelRepository labelRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final TaskMapper taskMapper;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository,
                       LabelRepository labelRepository,
                       UserRepository userRepository,
                       NotificationRepository notificationRepository,
                       TaskMapper taskMapper,
                       Clock clock) {
        this.taskRepository = taskRepository;
        this.labelRepository = labelRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.taskMapper = taskMapper;
        this.clock = clock;
    }

    // ---------------- Oxuma ----------------

    public TaskResponse get(Long ownerId, Long taskId) {
        return taskMapper.toResponse(findOwnedTask(ownerId, taskId));
    }

    public PageResponse<TaskResponse> search(Long ownerId, TaskFilter filter, Pageable pageable) {
        Page<Task> page = taskRepository.findAll(
                TaskSpecifications.fromFilter(ownerId, filter),
                withSafeSort(pageable)
        );
        return PageResponse.from(page, taskMapper::toResponse);
    }

    // ---------------- Yazma ----------------

    @Transactional
    public TaskResponse create(Long ownerId, TaskCreateRequest request) {
        Instant reminderAt = request.reminderAt();
        validateReminderNotAfterDeadline(reminderAt, request.deadline());
        if (reminderAt != null) {
            validateReminderInFuture(reminderAt);
        }

        List<Label> labels = findOwnedLabels(ownerId, request.labelIds());

        User owner = userRepository.getReferenceById(ownerId);
        Task task = taskMapper.toEntity(request, owner); // status = TODO
        task.replaceLabels(labels);
        if (reminderAt != null) {
            task.setReminderAt(reminderAt);
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long ownerId, Long taskId, TaskUpdateRequest request) {
        Task task = findOwnedTask(ownerId, taskId);

        Instant reminderAt = request.reminderAt();
        boolean reminderChanged = !Objects.equals(task.getReminderAt(), reminderAt);

        // Deadline yoxlaması həmişə: deadline reminder-dən əvvələ çəkilə bilər.
        validateReminderNotAfterDeadline(reminderAt, request.deadline());
        // Keçmiş vaxt yoxlaması yalnız dəyər dəyişəndə: köhnə reminder-i olan task redaktə oluna bilsin.
        if (reminderChanged && reminderAt != null) {
            validateReminderInFuture(reminderAt);
        }

        List<Label> labels = findOwnedLabels(ownerId, request.labelIds());

        taskMapper.updateEntity(request, task);
        task.replaceLabels(labels);
        // setReminderAt reminderSent-i sıfırlayır, ona görə yalnız dəyər dəyişəndə çağırılır.
        if (reminderChanged) {
            task.setReminderAt(reminderAt);
        }

        // flush: updatedAt və version response-da yeni dəyərlə görünsün,
        // optimistic lock xətası da burada çıxsın.
        taskRepository.flush();
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void delete(Long ownerId, Long taskId) {
        Task task = findOwnedTask(ownerId, taskId);
        task.markDeleted(Instant.now(clock));
        notificationRepository.deleteAllByTaskId(task.getId());
    }

    // ---------------- Status ----------------

    @Transactional
    public TaskResponse changeStatus(Long ownerId, Long taskId, TaskStatusUpdateRequest request) {
        Task task = findOwnedTask(ownerId, taskId);
        TaskStatus current = task.getStatus();
        TaskStatus target = request.status();

        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }

        task.changeStatus(target, Instant.now(clock));
        taskRepository.flush();
        return taskMapper.toResponse(task);
    }

    // ---------------- Label bağlama ----------------

    @Transactional
    public TaskResponse addLabel(Long ownerId, Long taskId, Long labelId) {
        Task task = findOwnedTask(ownerId, taskId);
        task.addLabel(findOwnedLabel(ownerId, labelId));
        taskRepository.flush();
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse removeLabel(Long ownerId, Long taskId, Long labelId) {
        Task task = findOwnedTask(ownerId, taskId);
        task.removeLabel(findOwnedLabel(ownerId, labelId));
        taskRepository.flush();
        return taskMapper.toResponse(task);
    }

    // ---------------- Köməkçi metodlar ----------------

    private Label findOwnedLabel(Long ownerId, Long labelId) {
        return labelRepository.findByIdAndOwnerId(labelId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found"));
    }

    private Task findOwnedTask(Long ownerId, Long taskId) {
        return taskRepository.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private List<Label> findOwnedLabels(Long ownerId, Set<Long> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return List.of();
        }
        List<Label> labels = labelRepository.findAllByIdInAndOwnerId(labelIds, ownerId);
        if (labels.size() != labelIds.size()) {
            throw new ResourceNotFoundException("One or more labels not found");
        }
        return labels;
    }

    private void validateReminderNotAfterDeadline(Instant reminderAt, Instant deadline) {
        if (reminderAt != null && deadline != null && reminderAt.isAfter(deadline)) {
            throw new BadRequestException("Reminder time must not be after the deadline");
        }
    }

    private void validateReminderInFuture(Instant reminderAt) {
        if (reminderAt.isBefore(Instant.now(clock))) {
            throw new BadRequestException("Reminder time must be in the future");
        }
    }

    private Pageable withSafeSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Sorting by '" + order.getProperty()
                        + "' is not allowed. Allowed fields: " + ALLOWED_SORT_FIELDS_TEXT);
            }
        }

        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(TIE_BREAKER));
    }
}
