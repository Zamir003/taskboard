package com.devlab.taskboard.dto.response;

import java.time.Instant;

public record LabelResponse(
        Long id,
        String name,
        Instant createdAt
) {
}
