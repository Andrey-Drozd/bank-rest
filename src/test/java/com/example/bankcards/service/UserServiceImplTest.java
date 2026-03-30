package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getAllUsersShouldReturnPagedUsers() {
        User user = createUser(1L, true, false, Set.of(role(RoleName.USER)));

        when(userRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));

        PageResponse<UserResponse> response = userService.getAllUsers("user", true, false, RoleName.USER, PageRequest.of(0, 10));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).email()).isEqualTo("user@bankcards.local");
        assertThat(response.content().get(0).roles()).containsExactly("USER");
    }

    @Test
    void updateUserShouldChangeFlagsAndRoles() {
        User user = createUser(1L, true, false, Set.of(role(RoleName.USER)));
        Role adminRole = role(RoleName.ADMIN);
        Role userRole = role(RoleName.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findAllByNameIn(Set.of(RoleName.ADMIN, RoleName.USER)))
                .thenReturn(List.of(adminRole, userRole));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateUser(
                99L,
                1L,
                new UpdateUserRequest(false, false, Set.of(RoleName.ADMIN, RoleName.USER))
        );

        assertThat(user.getEnabled()).isFalse();
        assertThat(user.getDeleted()).isFalse();
        assertThat(user.getRoles()).containsExactlyInAnyOrder(adminRole, userRole);
        assertThat(response.roles()).containsExactly("ADMIN", "USER");
    }

    @Test
    void updateUserShouldRejectUnknownRole() {
        User user = createUser(1L, true, false, Set.of(role(RoleName.USER)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findAllByNameIn(Set.of(RoleName.ADMIN, RoleName.USER)))
                .thenReturn(List.of(role(RoleName.ADMIN)));

        assertThatThrownBy(() -> userService.updateUser(
                99L,
                1L,
                new UpdateUserRequest(true, false, Set.of(RoleName.ADMIN, RoleName.USER))
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("One or more roles are invalid");
    }

    @Test
    void deleteUserShouldDisableAndSoftDeleteUser() {
        User user = createUser(1L, true, false, Set.of(role(RoleName.USER)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(99L, 1L);

        assertThat(user.getDeleted()).isTrue();
        assertThat(user.getEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void updateUserShouldRejectSelfManagement() {
        assertThatThrownBy(() -> userService.updateUser(
                1L,
                1L,
                new UpdateUserRequest(false, true, Set.of(RoleName.ADMIN))
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("You cannot manage your own account through admin endpoints");
    }

    @Test
    void deleteUserShouldRejectSelfDelete() {
        assertThatThrownBy(() -> userService.deleteUser(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("You cannot manage your own account through admin endpoints");
    }

    private User createUser(Long id, boolean enabled, boolean deleted, Set<Role> roles) {
        User user = new User();
        user.setId(id);
        user.setUsername("user");
        user.setEmail("user@bankcards.local");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(enabled);
        user.setDeleted(deleted);
        user.setRoles(roles);
        user.setCreatedAt(LocalDateTime.of(2026, 3, 26, 12, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 3, 26, 12, 5));
        return user;
    }

    private Role role(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(roleName.name());
        return role;
    }
}
