package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.dto.auth.UserProfileResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerShouldCreateUserWithUserRole() {
        RegisterRequest request = new RegisterRequest(
                "john",
                "john@example.com",
                "John",
                "Smith",
                "password123"
        );
        Role userRole = role(RoleName.USER);

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.roles()).containsExactly("USER");
        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest(
                "john",
                "john@example.com",
                "John",
                "Smith",
                "password123"
        );

        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username already exists");
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "john",
                "john@example.com",
                "John",
                "Smith",
                "password123"
        );

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already exists");
    }

    @Test
    void loginShouldAuthenticateAndReturnToken() {
        User user = user(1L, "john", "john@example.com", Set.of(role(RoleName.USER)));

        when(userRepository.findWithRolesByEmailAndDeletedFalse("john@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("john@example.com", "password123"));

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("john@example.com", "password123")
        );
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@example.com", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void currentUserShouldReturnProfile() {
        User user = user(1L, "john", "john@example.com", Set.of(role(RoleName.ADMIN), role(RoleName.USER)));

        when(userRepository.findWithRolesByEmailAndDeletedFalse("john@example.com")).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.currentUser("john@example.com");

        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
    }

    private User user(Long id, String username, String email, Set<Role> roles) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("John");
        user.setLastName("Smith");
        user.setPassword("encoded-password");
        user.setEnabled(true);
        user.setDeleted(false);
        user.setRoles(roles);
        return user;
    }

    private Role role(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(roleName.name());
        return role;
    }
}
