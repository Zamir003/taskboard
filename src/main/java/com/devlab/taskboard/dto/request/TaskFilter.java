package com.devlab.taskboard.dto.request;

import com.devlab.taskboard.enums.TaskStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record TaskFilter(

        Set<TaskStatus> status,

        @Positive(message = "Label id must be positive")
        Long labelId,

        Instant deadlineFrom,

        Instant deadlineTo,

        @Size(max = 100, message = "Keyword must be at most 100 characters")
        String keyword
) {

    @AssertTrue(message = "deadlineFrom must not be after deadlineTo")
    public boolean isDeadlineRangeValid() {
        return deadlineFrom == null || deadlineTo == null || !deadlineFrom.isAfter(deadlineTo);
    }
}
