package com.devlab.taskboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record TaskCreateRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        Instant deadline,

        Instant reminderAt,

        @Size(max = 50, message = "At most 50 labels can be assigned at once")
        Set<@NotNull(message = "Label id must not be null")
            @Positive(message = "Label id must be positive") Long> labelIds
) {
}
