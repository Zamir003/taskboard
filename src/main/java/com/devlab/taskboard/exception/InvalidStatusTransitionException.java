package com.devlab.taskboard.exception;

import com.devlab.taskboard.enums.TaskStatus;
import org.springframework.http.HttpStatus;

import java.util.stream.Collectors;

public class InvalidStatusTransitionException extends ApiException {

    public InvalidStatusTransitionException(TaskStatus from, TaskStatus to) {
        super(HttpStatus.CONFLICT, buildMessage(from, to));
    }

    private static String buildMessage(TaskStatus from, TaskStatus to) {
        String allowed = from.allowedTransitions().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        return "Cannot change status from " + from + " to " + to + ". Allowed: " + allowed;
    }
}
