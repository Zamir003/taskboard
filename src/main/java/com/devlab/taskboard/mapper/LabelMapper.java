package com.devlab.taskboard.mapper;

import com.devlab.taskboard.dto.response.LabelResponse;
import com.devlab.taskboard.entity.Label;
import com.devlab.taskboard.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LabelMapper {

    LabelResponse toResponse(Label label);

    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "name", source = "name")
    Label toEntity(User owner, String name);
}
