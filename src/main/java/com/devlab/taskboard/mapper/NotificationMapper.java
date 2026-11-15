package com.devlab.taskboard.mapper;

import com.devlab.taskboard.dto.response.NotificationResponse;
import com.devlab.taskboard.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface NotificationMapper {

    @Mapping(target = "taskId", source = "task.id")
    NotificationResponse toResponse(Notification notification);
}
