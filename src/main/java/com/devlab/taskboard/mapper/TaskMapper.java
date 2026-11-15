package com.devlab.taskboard.mapper;

import com.devlab.taskboard.dto.request.TaskCreateRequest;
import com.devlab.taskboard.dto.request.TaskUpdateRequest;
import com.devlab.taskboard.dto.response.LabelResponse;
import com.devlab.taskboard.dto.response.TaskResponse;
import com.devlab.taskboard.entity.Label;
import com.devlab.taskboard.entity.Task;
import com.devlab.taskboard.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(uses = LabelMapper.class)
public interface TaskMapper {

    @Mapping(target = "labels", source = "labels", qualifiedByName = "sortedLabels")
    TaskResponse toResponse(Task task);

    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "deadline", source = "request.deadline")
    @Mapping(target = "reminderAt", ignore = true)
    @Mapping(target = "labels", ignore = true)
    Task toEntity(TaskCreateRequest request, User owner);

    @Mapping(target = "reminderAt", ignore = true)
    @Mapping(target = "labels", ignore = true)
    void updateEntity(TaskUpdateRequest request, @MappingTarget Task task);

    @Named("sortedLabels")
    default List<LabelResponse> sortedLabels(Set<Label> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }
        List<Label> sorted = labels.stream()
                .sorted(Comparator.comparing(Label::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Label::getId))
                .toList();
        return toLabelResponses(sorted);
    }

    List<LabelResponse> toLabelResponses(List<Label> labels);
}
