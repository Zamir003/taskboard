package com.devlab.taskboard.controller;

import com.devlab.taskboard.dto.response.NotificationResponse;
import com.devlab.taskboard.dto.response.PageResponse;
import com.devlab.taskboard.dto.response.UnreadCountResponse;
import com.devlab.taskboard.security.UserPrincipal;
import com.devlab.taskboard.service.NotificationService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Sıra həmişə createdAt DESC, id DESC-dir (service-də). Ona görə sort Swagger-də gizlədilir. */
    @GetMapping
    @Parameter(name = "sort", hidden = true)
    public PageResponse<NotificationResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                                   @ParameterObject Pageable pageable) {
        return notificationService.list(principal.getId(), pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.unreadCount(principal.getId());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id) {
        return notificationService.markRead(principal.getId(), id);
    }
}
