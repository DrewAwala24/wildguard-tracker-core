package com.kws.backend.backend;

import com.kws.backend.backend.model.UserAccount;
import com.kws.backend.backend.service.UserService;
import com.kws.backend.backend.service.UserService.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthAndUserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("Authenticate with valid BCrypt credentials succeeds")
    void testValidAuthentication() {
        UserAccount user = userService.authenticate("commander", "kws123");
        assertNotNull(user);
        assertEquals("commander", user.getUsername());
        assertEquals("OPERATIONS_COMMANDER", user.getRole());
        assertNotNull(user.getLastLoginAt());
        assertEquals(user, userService.getCurrentUser());
    }

    @Test
    @DisplayName("Authenticate with invalid password throws SecurityException")
    void testInvalidPassword() {
        assertThrows(SecurityException.class, () -> {
            userService.authenticate("commander", "wrong_password");
        });
    }

    @Test
    @DisplayName("Authenticate with non-existent user throws SecurityException")
    void testNonExistentUser() {
        assertThrows(SecurityException.class, () -> {
            userService.authenticate("ghost_ranger", "kws123");
        });
    }

    @Test
    @DisplayName("Role-based permission enforcement checks work correctly")
    void testRolePermissions() {
        // Administrator has all permissions
        assertTrue(userService.hasPermission("ADMINISTRATOR", Permission.MANAGE_USERS));
        assertTrue(userService.hasPermission("ADMINISTRATOR", Permission.DISPATCH_PATROL));
        assertTrue(userService.hasPermission("ADMINISTRATOR", Permission.REGISTER_WILDLIFE));

        // Operations Commander has operational permissions except managing users
        assertTrue(userService.hasPermission("OPERATIONS_COMMANDER", Permission.DISPATCH_PATROL));
        assertTrue(userService.hasPermission("OPERATIONS_COMMANDER", Permission.REGISTER_WILDLIFE));
        assertFalse(userService.hasPermission("OPERATIONS_COMMANDER", Permission.MANAGE_USERS));

        // Dispatcher can dispatch and view data but cannot modify geofences or manage users
        assertTrue(userService.hasPermission("DISPATCHER", Permission.DISPATCH_PATROL));
        assertTrue(userService.hasPermission("DISPATCHER", Permission.VIEW_DATA));
        assertFalse(userService.hasPermission("DISPATCHER", Permission.MODIFY_GEOFENCE));
        assertFalse(userService.hasPermission("DISPATCHER", Permission.MANAGE_USERS));

        // Analyst is read-only / export
        assertTrue(userService.hasPermission("ANALYST", Permission.VIEW_DATA));
        assertTrue(userService.hasPermission("ANALYST", Permission.EXPORT_REPORT));
        assertFalse(userService.hasPermission("ANALYST", Permission.DISPATCH_PATROL));
        assertFalse(userService.hasPermission("ANALYST", Permission.REGISTER_WILDLIFE));
    }

    @Test
    @DisplayName("User registration with BCrypt hashing and deactivation toggle")
    void testUserRegistrationAndToggle() {
        // First authenticate as admin
        userService.authenticate("admin", "kws123");

        UserAccount newRanger = new UserAccount();
        newRanger.setUsername("field_ranger_test");
        newRanger.setPassword("plain_password_123");
        newRanger.setRole("RANGER");
        newRanger.setFullName("Test Field Ranger");
        newRanger.setContactEmail("ranger.test@kws.go.ke");

        UserAccount saved = userService.registerUser(newRanger);
        assertNotNull(saved.getId());
        assertTrue(saved.getPassword().startsWith("$2"), "Password must be BCrypt hashed");

        // Verify authentication with the new hashed password
        UserAccount auth = userService.authenticate("field_ranger_test", "plain_password_123");
        assertNotNull(auth);

        // Switch back to admin and toggle deactivation
        userService.authenticate("admin", "kws123");
        userService.toggleUserActive(saved.getId());

        // Deactivated user should be rejected
        assertThrows(SecurityException.class, () -> {
            userService.authenticate("field_ranger_test", "plain_password_123");
        });
    }
}
