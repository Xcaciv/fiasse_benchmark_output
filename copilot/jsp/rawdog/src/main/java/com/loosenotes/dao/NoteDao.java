package com.loosenotes.dao;

import com.loosenotes.db.ConnectionFactory;
import com.loosenotes.model.AdminNoteView;
import com.loosenotes.model.Note;
import com.loosenotes.model.NoteSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteDao extends BaseDao {
    public Note create(long userId, String title, String content, boolean isPublic) {
        String timestamp = now();
        String sql = "INSERT INTO notes(user_id, title, content, is_public, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.setInt(4, isPublic ? 1 : 0);
            statement.setString(5, timestamp);
            statement.setString(6, timestamp);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
            throw new IllegalStateException("Note creation did not return an id.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to create note.", ex);
        }
    }

    public Optional<Note> findById(long id) {
        String sql = "SELECT * FROM notes WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load note.", ex);
        }
    }

    public void update(long id, String title, String content, boolean isPublic) {
        String sql = "UPDATE notes SET title = ?, content = ?, is_public = ?, updated_at = ? WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setInt(3, isPublic ? 1 : 0);
            statement.setString(4, now());
            statement.setLong(5, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update note.", ex);
        }
    }

    public void delete(long id) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM notes WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete note.", ex);
        }
    }

    public List<NoteSummary> listOwnedByUser(long userId) {
        String sql = summaryQuery("WHERE n.user_id = ?") + " ORDER BY n.updated_at DESC";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            return readSummaries(statement);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load notes.", ex);
        }
    }

    public List<NoteSummary> searchVisible(String query, Long userId) {
        boolean hasQuery = query != null && !query.isBlank();
        String visibility = userId == null ? "n.is_public = 1" : "(n.is_public = 1 OR n.user_id = ?)";
        String sql = summaryQuery("WHERE " + visibility + " AND (? = 0 OR lower(n.title) LIKE ? OR lower(n.content) LIKE ?)")
                + (userId == null ? " ORDER BY n.updated_at DESC" : " ORDER BY CASE WHEN n.user_id = ? THEN 0 ELSE 1 END, n.updated_at DESC");
        String like = "%" + (hasQuery ? query.trim().toLowerCase() : "") + "%";

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (userId != null) {
                statement.setLong(index++, userId);
            }
            statement.setInt(index++, hasQuery ? 1 : 0);
            statement.setString(index++, like);
            statement.setString(index++, like);
            if (userId != null) {
                statement.setLong(index, userId);
            }
            return readSummaries(statement);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to search notes.", ex);
        }
    }

    public List<NoteSummary> topRatedPublicNotes() {
        String sql = summaryQuery("WHERE n.is_public = 1")
                + " HAVING COUNT(r.id) >= 3 ORDER BY AVG(r.rating) DESC, COUNT(r.id) DESC, n.updated_at DESC";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return readSummaries(statement);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load top-rated notes.", ex);
        }
    }

    public List<AdminNoteView> listRecentNotesForAdmin(int limit) {
        String sql = "SELECT n.id, n.title, n.user_id, u.username, n.is_public, n.created_at "
                + "FROM notes n JOIN users u ON u.id = n.user_id "
                + "ORDER BY n.created_at DESC LIMIT ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AdminNoteView> notes = new ArrayList<>();
                while (resultSet.next()) {
                    AdminNoteView note = new AdminNoteView();
                    note.setNoteId(resultSet.getLong("id"));
                    note.setTitle(resultSet.getString("title"));
                    note.setOwnerUserId(resultSet.getLong("user_id"));
                    note.setOwnerUsername(resultSet.getString("username"));
                    note.setPublic(resultSet.getInt("is_public") == 1);
                    note.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
                    notes.add(note);
                }
                return notes;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load recent notes.", ex);
        }
    }

    public void reassignOwner(long noteId, long newUserId) {
        String sql = "UPDATE notes SET user_id = ?, updated_at = ? WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, newUserId);
            statement.setString(2, now());
            statement.setLong(3, noteId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to reassign note ownership.", ex);
        }
    }

    public long countNotes() {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM notes");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to count notes.", ex);
        }
    }

    private String summaryQuery(String whereClause) {
        return "SELECT n.id, n.user_id, n.title, n.content, u.username, n.is_public, n.created_at, n.updated_at, "
                + "COALESCE(AVG(r.rating), 0) AS average_rating, COUNT(r.id) AS rating_count "
                + "FROM notes n "
                + "JOIN users u ON u.id = n.user_id "
                + "LEFT JOIN ratings r ON r.note_id = n.id "
                + whereClause
                + " GROUP BY n.id, n.user_id, n.title, n.content, u.username, n.is_public, n.created_at, n.updated_at";
    }

    private List<NoteSummary> readSummaries(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<NoteSummary> summaries = new ArrayList<>();
            while (resultSet.next()) {
                NoteSummary summary = new NoteSummary();
                summary.setId(resultSet.getLong("id"));
                summary.setOwnerUserId(resultSet.getLong("user_id"));
                summary.setTitle(resultSet.getString("title"));
                summary.setExcerpt(excerpt(resultSet.getString("content")));
                summary.setAuthorUsername(resultSet.getString("username"));
                summary.setPublic(resultSet.getInt("is_public") == 1);
                summary.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
                summary.setUpdatedAt(parseTimestamp(resultSet.getString("updated_at")));
                summary.setAverageRating(resultSet.getDouble("average_rating"));
                summary.setRatingCount(resultSet.getInt("rating_count"));
                summaries.add(summary);
            }
            return summaries;
        }
    }

    private Note map(ResultSet resultSet) throws SQLException {
        Note note = new Note();
        note.setId(resultSet.getLong("id"));
        note.setUserId(resultSet.getLong("user_id"));
        note.setTitle(resultSet.getString("title"));
        note.setContent(resultSet.getString("content"));
        note.setPublic(resultSet.getInt("is_public") == 1);
        note.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
        note.setUpdatedAt(parseTimestamp(resultSet.getString("updated_at")));
        return note;
    }
}
