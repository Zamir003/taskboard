package com.devlab.taskboard.dto.response;

import com.devlab.taskboard.enums.TaskStatus;

import java.time.Instant;
import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Instant deadline,
        Instant completedAt,
        Instant reminderAt,
        List<LabelResponse> labels,
        Instant createdAt,
        Instant updatedAt
) {
}
