package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(
            String query,
            Boolean enabled,
            Boolean deleted,
            RoleName role,
            Pageable pageable
    ) {
        Page<UserResponse> page = userRepository.findAll(buildSpecification(query, enabled, deleted, role), pageable)
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(getUserById(userId));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long actorUserId, Long userId, UpdateUserRequest request) {
        assertNotSelfManaged(actorUserId, userId);

        User user = getUserById(userId);

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        if (request.deleted() != null) {
            user.setDeleted(request.deleted());
            if (Boolean.TRUE.equals(request.deleted())) {
                user.setEnabled(false);
            }
        }

        if (request.roles() != null) {
            user.setRoles(resolveRoles(request.roles()));
        }

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long actorUserId, Long userId) {
        assertNotSelfManaged(actorUserId, userId);

        User user = getUserById(userId);
        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);
    }

    private void assertNotSelfManaged(Long actorUserId, Long userId) {
        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new ConflictException("You cannot manage your own account through admin endpoints");
        }
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        List<Role> roles = roleRepository.findAllByNameIn(roleNames);

        if (roles.size() != roleNames.size()) {
            throw new ConflictException("One or more roles are invalid");
        }

        return Set.copyOf(roles);
    }

    private Specification<User> buildSpecification(String query, Boolean enabled, Boolean deleted, RoleName role) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            criteriaQuery.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), pattern)
                ));
            }

            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }

            if (deleted != null) {
                predicates.add(criteriaBuilder.equal(root.get("deleted"), deleted));
            }

            if (role != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("roles", JoinType.LEFT).get("name"),
                        role
                ));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private UserResponse toResponse(User user) {
        List<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getEnabled(),
                user.getDeleted(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
