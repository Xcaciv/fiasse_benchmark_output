package com.loosenotes.dao;

import com.loosenotes.db.ConnectionFactory;
import com.loosenotes.model.Rating;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RatingDao extends BaseDao {
    public void upsert(long noteId, long userId, int ratingValue, String comment) {
        String timestamp = now();
        String sql = "INSERT INTO ratings(note_id, user_id, rating, comment, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(note_id, user_id) DO UPDATE SET "
                + "rating = excluded.rating, comment = excluded.comment, updated_at = excluded.updated_at";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setLong(2, userId);
            statement.setInt(3, ratingValue);
            statement.setString(4, comment);
            statement.setString(5, timestamp);
            statement.setString(6, timestamp);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save rating.", ex);
        }
    }

    public List<Rating> listByNoteId(long noteId) {
        String sql = "SELECT r.*, u.username FROM ratings r JOIN users u ON u.id = r.user_id WHERE r.note_id = ? ORDER BY r.updated_at DESC";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Rating> ratings = new ArrayList<>();
                while (resultSet.next()) {
                    ratings.add(map(resultSet));
                }
                return ratings;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load ratings.", ex);
        }
    }

    public Optional<Rating> findByNoteAndUser(long noteId, long userId) {
        String sql = "SELECT r.*, u.username FROM ratings r JOIN users u ON u.id = r.user_id WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setLong(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find rating.", ex);
        }
    }

    private Rating map(ResultSet resultSet) throws SQLException {
        Rating rating = new Rating();
        rating.setId(resultSet.getLong("id"));
        rating.setNoteId(resultSet.getLong("note_id"));
        rating.setUserId(resultSet.getLong("user_id"));
        rating.setUsername(resultSet.getString("username"));
        rating.setRating(resultSet.getInt("rating"));
        rating.setComment(resultSet.getString("comment"));
        rating.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
        rating.setUpdatedAt(parseTimestamp(resultSet.getString("updated_at")));
        return rating;
    }
}
