package com.devlab.taskboard.service;

import com.devlab.taskboard.entity.Label;
import com.devlab.taskboard.entity.Task;
import com.devlab.taskboard.enums.TaskStatus;
import com.devlab.taskboard.repository.TaskRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TaskExportService {

    private static final String[] HEADERS = {
            "id", "title", "description", "labels", "deadline", "completedAt", "createdAt"
    };

    private static final String LABEL_SEPARATOR = "; ";

    private static final char UTF8_BOM = '﻿';

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(HEADERS)
            .get();

    private static final Comparator<Label> LABEL_ORDER = Comparator
            .comparing((Label label) -> label.getName().toLowerCase(Locale.ROOT))
            .thenComparing(Label::getId);

    private final TaskRepository taskRepository;

    public TaskExportService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public String exportDoneTasks(Long ownerId) {
        List<Task> tasks = taskRepository.findAllByOwnerIdAndStatusOrderByCompletedAtDesc(ownerId, TaskStatus.DONE);

        StringWriter out = new StringWriter();
        out.write(UTF8_BOM);
        try (CSVPrinter printer = new CSVPrinter(out, FORMAT)) {
            for (Task task : tasks) {
                printer.printRecord(
                        task.getId(),
                        sanitize(task.getTitle()),
                        sanitize(task.getDescription()),
                        sanitize(joinLabels(task)),
                        format(task.getDeadline()),
                        format(task.getCompletedAt()),
                        format(task.getCreatedAt())
                );
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return out.toString();
    }

    private static String joinLabels(Task task) {
        return task.getLabels().stream()
                .sorted(LABEL_ORDER)
                .map(Label::getName)
                .collect(Collectors.joining(LABEL_SEPARATOR));
    }

    private static String format(Instant instant) {
        return instant == null ? "" : instant.toString();
    }

    private static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }
}
