package com.loosenotes.service;

import com.loosenotes.dao.UserDao;
import com.loosenotes.model.Role;
import com.loosenotes.model.User;
import com.loosenotes.util.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService – auth and registration logic.
 *
 * SSEM: Testability – dependencies mocked via constructor injection;
 * no database or container needed.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserDao userDao;
    @Mock private AuditService auditService;

    private RateLimiter rateLimiter;
    private UserService userService;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(5, 300);
        userService = new UserService(userDao, auditService, rateLimiter);
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @Test
    void register_throwsOnInvalidUsername() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.register("bad user!", "a@b.com", "password1".toCharArray(), "1.2.3.4"));
        assertTrue(ex.getMessage().contains("username"));
    }

    @Test
    void register_throwsOnInvalidEmail() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.register("gooduser", "notanemail", "password1".toCharArray(), "1.2.3.4"));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    void register_throwsOnWeakPassword() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.register("gooduser", "a@b.com", "short".toCharArray(), "1.2.3.4"));
        assertTrue(ex.getMessage().contains("8"));
    }

    @Test
    void register_throwsIfUsernameAlreadyTaken() throws Exception {
        User existing = buildUser("gooduser", "other@b.com");
        when(userDao.findByUsername("gooduser")).thenReturn(Optional.of(existing));

        assertThrows(ServiceException.class, () ->
                userService.register("gooduser", "new@b.com", "password1".toCharArray(), "1.2.3.4"));
    }

    @Test
    void register_createsUserOnValidInput() throws Exception {
        when(userDao.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userDao.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userDao.insert(any(User.class))).thenReturn(42L);

        long id = userService.register("newuser", "new@example.com",
                "password1234".toCharArray(), "1.2.3.4");

        assertEquals(42L, id);
        verify(userDao).insert(argThat(u ->
                "newuser".equals(u.getUsername()) &&
                "new@example.com".equals(u.getEmail()) &&
                Role.USER == u.getRole() &&
                u.isEnabled()));
    }

    // ── Authentication ────────────────────────────────────────────────────────

    @Test
    void authenticate_throwsOnRateLimitExceeded() throws Exception {
        // Exhaust the limiter
        for (int i = 0; i < 5; i++) rateLimiter.tryAcquire("1.1.1.1");

        assertThrows(ServiceException.class, () ->
                userService.authenticate("user", "pass".toCharArray(), "1.1.1.1"));
    }

    @Test
    void authenticate_throwsOnUnknownUser() throws Exception {
        when(userDao.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () ->
                userService.authenticate("unknown", "pw12345678".toCharArray(), "2.2.2.2"));
    }

    @Test
    void authenticate_throwsOnDisabledAccount() throws Exception {
        User disabled = buildUser("alice", "alice@example.com");
        disabled.setEnabled(false);
        // Hash for "password1" so the password check passes
        disabled.setPasswordHash(com.loosenotes.util.PasswordUtil.hash("password1".toCharArray()));
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(disabled));

        assertThrows(ServiceException.class, () ->
                userService.authenticate("alice", "password1".toCharArray(), "3.3.3.3"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String username, String email) {
        User u = new User();
        u.setId(1L);
        u.setUsername(username);
        u.setEmail(email);
        u.setRole(Role.USER);
        u.setEnabled(true);
        u.setPasswordHash(com.loosenotes.util.PasswordUtil.hash("password1".toCharArray()));
        return u;
    }
}
