package com.loosenotes.service;

import com.loosenotes.dao.UserDao;
import com.loosenotes.model.Role;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * SSEM: Testability - UserService depends on UserDao interface; fully mockable.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDao);
    }

    @Test
    void register_validInputs_createsUser() throws SQLException {
        when(userDao.findByUsername("alice")).thenReturn(Optional.empty());
        when(userDao.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(userDao.create(any(User.class))).thenReturn(1L);

        Optional<User> result = userService.register("alice", "alice@example.com", "P@ssw0rd1");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
        verify(userDao).create(any(User.class));
    }

    @Test
    void register_duplicateUsername_returnsEmpty() throws SQLException {
        User existing = new User();
        existing.setUsername("alice");
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(existing));

        Optional<User> result = userService.register("alice", "other@example.com", "P@ssw0rd1");

        assertTrue(result.isEmpty());
        verify(userDao, never()).create(any());
    }

    @Test
    void register_invalidUsername_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> userService.register("a b", "email@example.com", "P@ssw0rd1"));
    }

    @Test
    void register_weakPassword_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> userService.register("alice", "alice@example.com", "weak"));
    }

    @Test
    void authenticate_correctPassword_returnsSuccess() throws SQLException {
        User user = createTestUser("alice", "P@ssw0rd1");
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(user));

        UserService.AuthResult result = userService.authenticate("alice", "P@ssw0rd1");

        assertEquals(UserService.AuthResult.SUCCESS, result);
        verify(userDao).resetFailedLogins(user.getId());
    }

    @Test
    void authenticate_wrongPassword_returnsInvalidCredentials() throws SQLException {
        User user = createTestUser("alice", "P@ssw0rd1");
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(user));

        UserService.AuthResult result = userService.authenticate("alice", "WrongP@ss1");

        assertEquals(UserService.AuthResult.INVALID_CREDENTIALS, result);
    }

    @Test
    void authenticate_unknownUser_returnsNotFound() throws SQLException {
        when(userDao.findByUsername("unknown")).thenReturn(Optional.empty());

        UserService.AuthResult result = userService.authenticate("unknown", "P@ssw0rd1");

        assertEquals(UserService.AuthResult.NOT_FOUND, result);
    }

    @Test
    void authenticate_lockedAccount_returnsLocked() throws SQLException {
        User user = createTestUser("alice", "P@ssw0rd1");
        user.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(10));
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(user));

        UserService.AuthResult result = userService.authenticate("alice", "P@ssw0rd1");

        assertEquals(UserService.AuthResult.ACCOUNT_LOCKED, result);
    }

    private User createTestUser(String username, String password) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(Role.USER);
        return user;
    }
}
