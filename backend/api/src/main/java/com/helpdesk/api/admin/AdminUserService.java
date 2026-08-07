package com.helpdesk.api.admin;

import com.helpdesk.api.domain.Role;
import com.helpdesk.api.domain.User;
import com.helpdesk.api.repository.RoleRepository;
import com.helpdesk.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {

    private static final Set<String> VALID_ROLES = Set.of("CLIENT", "AGENT", "MANAGER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(UserResponse::of)
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        validateRole(req.role());
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja folosit");
        }
        Role role = requireRole(req.role());
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .role(role)
                .active(true)
                .build();
        userRepository.save(user);
        return UserResponse.of(user);
    }

    @Transactional
    public UserResponse changeRole(Long id, ChangeRoleRequest req) {
        validateRole(req.role());
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizator inexistent"));
        Role role = requireRole(req.role());
        user.setRole(role);
        return UserResponse.of(userRepository.save(user));
    }

    private void validateRole(String role) {
        if (!VALID_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol invalid: " + role);
        }
    }

    private Role requireRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Rolul " + name + " lipseste din baza de date"));
    }
}