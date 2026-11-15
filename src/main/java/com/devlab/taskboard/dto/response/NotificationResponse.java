package com.devlab.taskboard.dto.response;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long taskId,
        String message,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
