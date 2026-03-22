package com.loosenotes.dao;

import com.loosenotes.model.User;
import java.util.List;
import java.util.Optional;

/** Data access contract for users. Implementations use parameterized queries only. */
public interface UserDao {
    long insert(User user);
    Optional<User> findById(long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String token);
    boolean update(User user);
    boolean updatePassword(long userId, String newHash);
    boolean setResetToken(long userId, String token, String expiresAt);
    boolean clearResetToken(long userId);
    List<User> findAll();
    List<User> searchByUsernameOrEmail(String query);
    int countAll();
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    int countNotesByUserId(long userId);
}
