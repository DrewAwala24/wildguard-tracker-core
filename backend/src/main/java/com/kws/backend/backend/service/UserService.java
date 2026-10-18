package com.kws.backend.backend.service;

import com.kws.backend.backend.model.UserAccount;
import com.kws.backend.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    // In-process authenticated session context for Desktop UI
    private volatile UserAccount currentUser;

    public enum Role {
        ADMINISTRATOR,
        OPERATIONS_COMMANDER,
        DISPATCHER,
        RANGER,
        ANALYST
    }

    public enum Permission {
        VIEW_DATA,
        REGISTER_WILDLIFE,
        EDIT_WILDLIFE,
        DISPATCH_PATROL,
        RESOLVE_INCIDENT,
        MODIFY_GEOFENCE,
        MANAGE_USERS,
        EXPORT_REPORT
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }

    public synchronized UserAccount authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            throw new IllegalArgumentException("Username and password must be provided.");
        }

        UserAccount user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new SecurityException("Invalid credentials for user: " + username));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new SecurityException("Account for user " + username + " is deactivated.");
        }

        boolean matches = false;
        if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$") || user.getPassword().startsWith("$2y$")) {
            matches = BCrypt.checkpw(rawPassword, user.getPassword());
        } else {
            // Fallback for legacy seeded passwords, auto-upgrade to BCrypt
            if (rawPassword.equals(user.getPassword())) {
                matches = true;
                user.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt(10)));
                userRepository.save(user);
            }
        }

        if (!matches) {
            auditService.logAction(username, "LOGIN_FAILED", "USER", String.valueOf(user.getId()), "Failed login attempt with invalid password.");
            throw new SecurityException("Invalid credentials for user: " + username);
        }

        user.setLastLoginAt(LocalDateTime.now());
        UserAccount updated = userRepository.save(user);
        this.currentUser = updated;

        auditService.logAction(username, "USER_LOGIN", "USER", String.valueOf(updated.getId()), "Successful login to Operations Command as " + updated.getRole());
        return updated;
    }

    public synchronized void logout() {
        if (currentUser != null) {
            auditService.logAction(currentUser.getUsername(), "USER_LOGOUT", "USER", String.valueOf(currentUser.getId()), "Logged out of session.");
            currentUser = null;
        }
    }

    public boolean hasPermission(Permission permission) {
        if (currentUser == null) {
            return false;
        }
        return hasPermission(currentUser.getRole(), permission);
    }

    public boolean hasPermission(String roleStr, Permission permission) {
        if (roleStr == null) return false;
        try {
            Role role = Role.valueOf(roleStr.toUpperCase());
            return switch (role) {
                case ADMINISTRATOR -> true;
                case OPERATIONS_COMMANDER -> permission != Permission.MANAGE_USERS;
                case DISPATCHER -> Set.of(Permission.VIEW_DATA, Permission.DISPATCH_PATROL, Permission.RESOLVE_INCIDENT, Permission.EXPORT_REPORT).contains(permission);
                case RANGER -> Set.of(Permission.VIEW_DATA, Permission.RESOLVE_INCIDENT).contains(permission);
                case ANALYST -> Set.of(Permission.VIEW_DATA, Permission.EXPORT_REPORT).contains(permission);
            };
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void enforcePermission(Permission permission) {
        if (!hasPermission(permission)) {
            String userRole = (currentUser != null) ? currentUser.getRole() : "ANONYMOUS";
            throw new SecurityException("Unauthorized: Role " + userRole + " does not possess permission " + permission);
        }
    }

    @Transactional
    public UserAccount registerUser(UserAccount user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
            user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10)));
        }
        if (user.getActive() == null) {
            user.setActive(true);
        }
        user.setCreatedAt(LocalDateTime.now());
        UserAccount saved = userRepository.save(user);
        auditService.logAction("ADMIN", "USER_REGISTERED", "USER", String.valueOf(saved.getId()),
                "Registered new user " + saved.getUsername() + " with role " + saved.getRole());
        return saved;
    }

    public List<UserAccount> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<UserAccount> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public void toggleUserActive(Long userId) {
        enforcePermission(Permission.MANAGE_USERS);
        userRepository.findById(userId).ifPresent(user -> {
            user.setActive(!Boolean.TRUE.equals(user.getActive()));
            userRepository.save(user);
            auditService.logAction(currentUser != null ? currentUser.getUsername() : "ADMIN",
                    "USER_STATUS_TOGGLED", "USER", String.valueOf(userId),
                    "User " + user.getUsername() + " active status set to " + user.getActive());
        });
    }
}