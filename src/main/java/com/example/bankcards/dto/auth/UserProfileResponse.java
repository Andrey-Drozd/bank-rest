package com.example.bankcards.dto.auth;

import java.util.List;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        List<String> roles
) {
}
