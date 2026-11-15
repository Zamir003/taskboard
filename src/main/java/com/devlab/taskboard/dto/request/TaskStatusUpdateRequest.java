package com.devlab.taskboard.dto.request;

import com.devlab.taskboard.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(

        @NotNull(message = "Status is required")
        TaskStatus status
) {
}
