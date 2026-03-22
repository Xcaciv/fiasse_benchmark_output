package com.loosenotes.service;

import com.loosenotes.model.User;
import java.util.List;
import java.util.Optional;

/** Business logic contract for user management. */
public interface UserService {
    /** Registers a new user; throws ServiceException on duplicate or validation failure. */
    User register(String username, String email, String password);

    /** Authenticates credentials; returns user on success, empty on failure. */
    Optional<User> authenticate(String username, String password);

    Optional<User> findById(long id);
    Optional<User> findByUsername(String username);

    /** Updates profile fields; throws ServiceException on conflict. */
    void updateProfile(long userId, String username, String email);

    /** Changes password after verifying current password. */
    void changePassword(long userId, String currentPassword, String newPassword);

    List<User> findAll();
    List<User> searchUsers(String query);
    int countAll();
    int countNotesByUser(long userId);
}
