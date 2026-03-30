package com.example.bankcards.dto.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled,
        Boolean deleted,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
