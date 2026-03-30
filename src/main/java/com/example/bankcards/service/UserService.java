package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.enums.RoleName;
import org.springframework.data.domain.Pageable;

public interface UserService {

    PageResponse<UserResponse> getAllUsers(String query, Boolean enabled, Boolean deleted, RoleName role, Pageable pageable);

    UserResponse getUser(Long userId);

    UserResponse updateUser(Long actorUserId, Long userId, UpdateUserRequest request);

    void deleteUser(Long actorUserId, Long userId);
}
