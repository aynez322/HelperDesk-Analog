package com.helpdesk.api.admin;

import com.helpdesk.api.domain.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String role,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse of(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole() != null ? u.getRole().getName() : null,
                u.isActive(),
                u.getCreatedAt()
        );
    }
}