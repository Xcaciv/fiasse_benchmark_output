package com.loosenotes.dao;

import com.loosenotes.model.Rating;
import com.loosenotes.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RatingDAO {

    private final UserDAO userDAO = new UserDAO();

    public boolean createRating(Rating rating) {
        String sql = "INSERT INTO ratings (note_id, user_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, rating.getNoteId());
            stmt.setLong(2, rating.getUserId());
            stmt.setInt(3, rating.getRating());
            stmt.setString(4, rating.getComment());
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    rating.setId(rs.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateRating(Rating rating) {
        String sql = "UPDATE ratings SET rating = ?, comment = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rating.getRating());
            stmt.setString(2, rating.getComment());
            stmt.setLong(3, rating.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Optional<Rating> findById(Long id) {
        String sql = "SELECT r.*, u.username as rater_username FROM ratings r LEFT JOIN users u ON r.user_id = u.id WHERE r.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Rating rating = mapResultSetToRating(rs);
                populateRater(conn, rating);
                return Optional.of(rating);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Rating> findByUserAndNote(Long userId, Long noteId) {
        String sql = "SELECT r.*, u.username as rater_username FROM ratings r LEFT JOIN users u ON r.user_id = u.id WHERE r.user_id = ? AND r.note_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, noteId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Rating rating = mapResultSetToRating(rs);
                populateRater(conn, rating);
                return Optional.of(rating);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Rating> findByNoteId(Long noteId) {
        List<Rating> ratings = new ArrayList<>();
        String sql = "SELECT r.*, u.username as rater_username FROM ratings r LEFT JOIN users u ON r.user_id = u.id WHERE r.note_id = ? ORDER BY r.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Rating rating = mapResultSetToRating(rs);
                populateRater(conn, rating);
                ratings.add(rating);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ratings;
    }

    public double getAverageRating(Long noteId) {
        String sql = "SELECT AVG(rating) FROM ratings WHERE note_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public int getRatingCount(Long noteId) {
        String sql = "SELECT COUNT(*) FROM ratings WHERE note_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean deleteRating(Long id) {
        String sql = "DELETE FROM ratings WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void populateRater(Connection conn, Rating rating) throws SQLException {
        userDAO.findById(rating.getUserId()).ifPresent(rating::setRater);
    }

    private Rating mapResultSetToRating(ResultSet rs) throws SQLException {
        Rating rating = new Rating();
        rating.setId(rs.getLong("id"));
        rating.setNoteId(rs.getLong("note_id"));
        rating.setUserId(rs.getLong("user_id"));
        rating.setRating(rs.getInt("rating"));
        rating.setComment(rs.getString("comment"));
        rating.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            rating.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        User rater = new User();
        rater.setUsername(rs.getString("rater_username"));
        rating.setRater(rater);
        
        return rating;
    }
}
