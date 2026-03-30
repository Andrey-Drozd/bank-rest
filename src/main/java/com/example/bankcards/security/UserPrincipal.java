package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record UserPrincipal(
        Long id,
        String username,
        String email,
        String password,
        boolean enabled,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    public static UserPrincipal fromUser(User user) {
        List<SimpleGrantedAuthority> grantedAuthorities = user.getRoles()
                .stream()
                .map(Role::getName)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getEnabled()) && !Boolean.TRUE.equals(user.getDeleted()),
                grantedAuthorities
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
