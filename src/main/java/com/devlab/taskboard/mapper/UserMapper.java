package com.devlab.taskboard.mapper;

import com.devlab.taskboard.dto.request.RegisterRequest;
import com.devlab.taskboard.dto.response.UserResponse;
import com.devlab.taskboard.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    User toEntity(RegisterRequest request, String email, String passwordHash);

    UserResponse toResponse(User user);
}
