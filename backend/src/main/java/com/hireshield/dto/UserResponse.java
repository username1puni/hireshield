package com.hireshield.dto;

import java.time.Instant;

import com.hireshield.entity.Role;
import com.hireshield.entity.User;

public record UserResponse(Long id, String name, String email, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
