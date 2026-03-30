package com.example.bankcards.dto.user;

import com.example.bankcards.entity.enums.RoleName;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        Boolean enabled,
        Boolean deleted,
        @Size(min = 1, max = 2) Set<RoleName> roles
) {
}
