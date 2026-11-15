package com.devlab.taskboard.controller;

import com.devlab.taskboard.dto.request.LabelRequest;
import com.devlab.taskboard.dto.response.LabelResponse;
import com.devlab.taskboard.security.UserPrincipal;
import com.devlab.taskboard.service.LabelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping
    public List<LabelResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return labelService.list(principal.getId());
    }

    @GetMapping("/{id}")
    public LabelResponse get(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable Long id) {
        return labelService.get(principal.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabelResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                @Valid @RequestBody LabelRequest request) {
        return labelService.create(principal.getId(), request);
    }

    @PutMapping("/{id}")
    public LabelResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable Long id,
                                @Valid @RequestBody LabelRequest request) {
        return labelService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long id) {
        labelService.delete(principal.getId(), id);
    }
}
