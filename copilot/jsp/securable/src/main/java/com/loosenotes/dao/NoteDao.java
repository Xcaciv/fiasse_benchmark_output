package com.loosenotes.dao;

import com.loosenotes.db.Database;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.AppUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteDao {
    private static final String NOTE_SELECT =
        "SELECT n.id, n.user_id, u.username AS owner_username, n.title, n.content, n.is_public, "
            + "n.created_at, n.updated_at, "
            + "COALESCE(AVG(r.rating_value), 0) AS average_rating, "
            + "COUNT(r.id) AS rating_count "
            + "FROM notes n "
            + "JOIN users u ON u.id = n.user_id "
            + "LEFT JOIN ratings r ON r.note_id = n.id ";

    public long createNote(long userId, String title, String content, boolean publicNote) throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, is_public) VALUES (?, ?, ?, ?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, AppUtil.trimToEmpty(title));
            statement.setString(3, AppUtil.trimToEmpty(content));
            statement.setInt(4, publicNote ? 1 : 0);
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to create note.");
    }

    public void updateNote(long noteId, String title, String content, boolean publicNote) throws SQLException {
        String sql = "UPDATE notes SET title = ?, content = ?, is_public = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AppUtil.trimToEmpty(title));
            statement.setString(2, AppUtil.trimToEmpty(content));
            statement.setInt(3, publicNote ? 1 : 0);
            statement.setLong(4, noteId);
            statement.executeUpdate();
        }
    }

    public void deleteNote(long noteId) throws SQLException {
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM notes WHERE id = ?")) {
            statement.setLong(1, noteId);
            statement.executeUpdate();
        }
    }

    public Optional<Note> findById(long noteId) throws SQLException {
        String sql = NOTE_SELECT + "WHERE n.id = ? GROUP BY n.id, u.username";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapNote(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Note> listOwnerNotes(long ownerId) throws SQLException {
        String sql = NOTE_SELECT + "WHERE n.user_id = ? GROUP BY n.id, u.username ORDER BY n.updated_at DESC, n.id DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ownerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        }
        return notes;
    }

    public List<Note> listRecentPublic(int limit) throws SQLException {
        String sql = NOTE_SELECT + "WHERE n.is_public = 1 GROUP BY n.id, u.username ORDER BY n.created_at DESC, n.id DESC LIMIT ?";
        List<Note> notes = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        }
        return notes;
    }

    public List<Note> searchVisibleNotes(String query, Long viewerUserId) throws SQLException {
        String sql = NOTE_SELECT + "WHERE (? = '' OR lower(n.title) LIKE ? OR lower(n.content) LIKE ?) AND (n.is_public = 1"
            + (viewerUserId != null ? " OR n.user_id = ?" : "") + ") GROUP BY n.id, u.username ORDER BY n.updated_at DESC, n.id DESC";
        String clean = AppUtil.trimToEmpty(query);
        String like = AppUtil.likeValue(clean);
        List<Note> notes = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setString(index++, clean);
            statement.setString(index++, like);
            statement.setString(index++, like);
            if (viewerUserId != null) {
                statement.setLong(index, viewerUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        }
        return notes;
    }

    public List<Note> listTopRatedNotes() throws SQLException {
        String sql = NOTE_SELECT + "WHERE n.is_public = 1 GROUP BY n.id, u.username HAVING COUNT(r.id) >= 3 "
            + "ORDER BY average_rating DESC, rating_count DESC, n.updated_at DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                notes.add(mapNote(rs));
            }
        }
        return notes;
    }

    public List<Note> listAdminNotes(String query) throws SQLException {
        String sql = NOTE_SELECT
            + "WHERE (? = '' OR lower(n.title) LIKE ? OR lower(n.content) LIKE ? OR lower(u.username) LIKE ?) "
            + "GROUP BY n.id, u.username ORDER BY n.updated_at DESC, n.id DESC LIMIT 50";
        String clean = AppUtil.trimToEmpty(query);
        String like = AppUtil.likeValue(clean);
        List<Note> notes = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clean);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        }
        return notes;
    }

    public long countNotes() throws SQLException {
        try (Connection connection = Database.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM notes")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    public void addAttachment(long noteId, String storedName, String originalName, String contentType, long sizeBytes) throws SQLException {
        String sql = "INSERT INTO attachments (note_id, stored_name, original_name, content_type, size_bytes) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setString(2, storedName);
            statement.setString(3, originalName);
            statement.setString(4, contentType);
            statement.setLong(5, sizeBytes);
            statement.executeUpdate();
        }
    }

    public List<Attachment> listAttachments(long noteId) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at DESC, id DESC";
        List<Attachment> attachments = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    attachments.add(mapAttachment(rs));
                }
            }
        }
        return attachments;
    }

    public Optional<Attachment> findAttachmentById(long attachmentId) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attachmentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAttachment(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void upsertRating(long noteId, long userId, int ratingValue, String comment) throws SQLException {
        String sql =
            "INSERT INTO ratings (note_id, user_id, rating_value, comment, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                + "ON CONFLICT(note_id, user_id) "
                + "DO UPDATE SET rating_value = excluded.rating_value, comment = excluded.comment, updated_at = CURRENT_TIMESTAMP";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setLong(2, userId);
            statement.setInt(3, ratingValue);
            statement.setString(4, AppUtil.trimToEmpty(comment));
            statement.executeUpdate();
        }
    }

    public List<Rating> listRatings(long noteId) throws SQLException {
        String sql =
            "SELECT r.id, r.note_id, r.user_id, u.username, r.rating_value, r.comment, r.created_at, r.updated_at "
                + "FROM ratings r "
                + "JOIN users u ON u.id = r.user_id "
                + "WHERE r.note_id = ? "
                + "ORDER BY r.updated_at DESC, r.id DESC";
        List<Rating> ratings = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ratings.add(mapRating(rs));
                }
            }
        }
        return ratings;
    }

    public Optional<Rating> findUserRating(long noteId, long userId) throws SQLException {
        String sql =
            "SELECT r.id, r.note_id, r.user_id, u.username, r.rating_value, r.comment, r.created_at, r.updated_at "
                + "FROM ratings r JOIN users u ON u.id = r.user_id "
                + "WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRating(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void revokeActiveShareLinks(long noteId) throws SQLException {
        String sql = "UPDATE share_links SET revoked_at = CURRENT_TIMESTAMP WHERE note_id = ? AND revoked_at IS NULL";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.executeUpdate();
        }
    }

    public void createShareLink(long noteId, String tokenHash) throws SQLException {
        String sql = "INSERT INTO share_links (note_id, token_hash) VALUES (?, ?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setString(2, tokenHash);
            statement.executeUpdate();
        }
    }

    public Optional<ShareLink> findActiveShareLinkForNote(long noteId) throws SQLException {
        String sql = "SELECT id, note_id, created_at, revoked_at FROM share_links WHERE note_id = ? AND revoked_at IS NULL ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ShareLink(rs.getLong("id"), rs.getLong("note_id"), rs.getString("created_at"), rs.getString("revoked_at")));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Note> findNoteByShareTokenHash(String tokenHash) throws SQLException {
        String sql = NOTE_SELECT + "JOIN share_links s ON s.note_id = n.id WHERE s.token_hash = ? AND s.revoked_at IS NULL GROUP BY n.id, u.username";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapNote(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean shareTokenGrantsAccess(long noteId, String tokenHash) throws SQLException {
        String sql = "SELECT COUNT(*) FROM share_links WHERE note_id = ? AND token_hash = ? AND revoked_at IS NULL";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setString(2, tokenHash);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
        return false;
    }

    public void reassignNote(long noteId, long newOwnerId) throws SQLException {
        String sql = "UPDATE notes SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, newOwnerId);
            statement.setLong(2, noteId);
            statement.executeUpdate();
        }
    }

    private Note mapNote(ResultSet rs) throws SQLException {
        String content = rs.getString("content");
        return new Note(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("owner_username"),
            rs.getString("title"),
            content,
            rs.getInt("is_public") == 1,
            rs.getString("created_at"),
            rs.getString("updated_at"),
            rs.getDouble("average_rating"),
            rs.getInt("rating_count"),
            AppUtil.excerpt(content, 200)
        );
    }

    private Attachment mapAttachment(ResultSet rs) throws SQLException {
        return new Attachment(
            rs.getLong("id"),
            rs.getLong("note_id"),
            rs.getString("stored_name"),
            rs.getString("original_name"),
            rs.getString("content_type"),
            rs.getLong("size_bytes"),
            rs.getString("created_at")
        );
    }

    private Rating mapRating(ResultSet rs) throws SQLException {
        return new Rating(
            rs.getLong("id"),
            rs.getLong("note_id"),
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getInt("rating_value"),
            rs.getString("comment"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        );
    }
}
