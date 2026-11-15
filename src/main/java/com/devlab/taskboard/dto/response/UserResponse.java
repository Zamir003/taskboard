package com.devlab.taskboard.dto.response;

import com.devlab.taskboard.enums.Role;
import com.devlab.taskboard.enums.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
