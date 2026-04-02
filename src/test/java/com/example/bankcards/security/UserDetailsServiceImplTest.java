package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldLoadUserWithAuthoritiesFromRolesQuery() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setPassword("encoded-password");
        user.setEnabled(true);
        user.setDeleted(false);
        user.setRoles(Set.of(role(RoleName.ADMIN), role(RoleName.USER)));

        when(userRepository.findWithRolesByEmailAndDeletedFalse("john@example.com"))
                .thenReturn(Optional.of(user));

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername("john@example.com");

        verify(userRepository).findWithRolesByEmailAndDeletedFalse("john@example.com");
        assertThat(principal.email()).isEqualTo("john@example.com");
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void shouldFailWhenUserDoesNotExist() {
        when(userRepository.findWithRolesByEmailAndDeletedFalse("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    private Role role(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(roleName.name());
        return role;
    }
}
