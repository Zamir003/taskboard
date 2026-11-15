package com.devlab.taskboard.controller;

import com.devlab.taskboard.dto.request.TaskCreateRequest;
import com.devlab.taskboard.dto.request.TaskFilter;
import com.devlab.taskboard.dto.request.TaskStatusUpdateRequest;
import com.devlab.taskboard.dto.request.TaskUpdateRequest;
import com.devlab.taskboard.dto.response.PageResponse;
import com.devlab.taskboard.dto.response.TaskResponse;
import com.devlab.taskboard.security.UserPrincipal;
import com.devlab.taskboard.service.TaskExportService;
import com.devlab.taskboard.service.TaskService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final String TEXT_CSV_VALUE = "text/csv";
    private static final MediaType TEXT_CSV = new MediaType(MediaType.parseMediaType(TEXT_CSV_VALUE), StandardCharsets.UTF_8);
    private static final String EXPORT_FILENAME = "tasks-done.csv";

    private final TaskService taskService;
    private final TaskExportService taskExportService;

    public TaskController(TaskService taskService, TaskExportService taskExportService) {
        this.taskService = taskService;
        this.taskExportService = taskExportService;
    }

    /**
     * DONE taskların CSV-si fayl kimi endirilir (text/csv + attachment, ad: tasks-done.csv).
     * "/export" literal path olduğu üçün "/{id}"-dən üstündür.
     */
    @GetMapping(value = "/export", produces = TEXT_CSV_VALUE)
    public ResponseEntity<String> exportDone(@AuthenticationPrincipal UserPrincipal principal) {
        String csv = taskExportService.exportDoneTasks(principal.getId());
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(EXPORT_FILENAME).build().toString())
                .body(csv);
    }

    @GetMapping
    public PageResponse<TaskResponse> search(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @ParameterObject TaskFilter filter,
                                             @ParameterObject Pageable pageable) {
        return taskService.search(principal.getId(), filter, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@AuthenticationPrincipal UserPrincipal principal,
                            @PathVariable Long id) {
        return taskService.get(principal.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody TaskCreateRequest request) {
        return taskService.create(principal.getId(), request);
    }

    /** Tam əvəzləmə: null sahələr təmizlənir, labelIds label-ləri tam əvəz edir. */
    @PutMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long id,
                               @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long id) {
        taskService.delete(principal.getId(), id);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id,
                                     @Valid @RequestBody TaskStatusUpdateRequest request) {
        return taskService.changeStatus(principal.getId(), id, request);
    }

    /** İdempotent: label artıq bağlıdırsa, heç nə dəyişmir. */
    @PostMapping("/{id}/labels/{labelId}")
    public TaskResponse addLabel(@AuthenticationPrincipal UserPrincipal principal,
                                 @PathVariable Long id,
                                 @PathVariable Long labelId) {
        return taskService.addLabel(principal.getId(), id, labelId);
    }

    /** İdempotent: label bağlı deyilsə, heç nə dəyişmir. */
    @DeleteMapping("/{id}/labels/{labelId}")
    public TaskResponse removeLabel(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id,
                                    @PathVariable Long labelId) {
        return taskService.removeLabel(principal.getId(), id, labelId);
    }
}
