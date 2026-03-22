package com.loosenotes.dao;

import com.loosenotes.db.ConnectionFactory;
import com.loosenotes.model.Attachment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttachmentDao extends BaseDao {
    public Attachment create(long noteId, String storageName, String originalFilename, String contentType, long fileSize) {
        String sql = "INSERT INTO attachments(note_id, storage_name, original_filename, content_type, file_size, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, noteId);
            statement.setString(2, storageName);
            statement.setString(3, originalFilename);
            statement.setString(4, contentType);
            statement.setLong(5, fileSize);
            statement.setString(6, now());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
            throw new IllegalStateException("Attachment creation did not return an id.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save attachment metadata.", ex);
        }
    }

    public List<Attachment> listByNoteId(long noteId) {
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at ASC";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Attachment> attachments = new ArrayList<>();
                while (resultSet.next()) {
                    attachments.add(map(resultSet));
                }
                return attachments;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load attachments.", ex);
        }
    }

    public Optional<Attachment> findById(long id) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM attachments WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find attachment.", ex);
        }
    }

    private Attachment map(ResultSet resultSet) throws SQLException {
        Attachment attachment = new Attachment();
        attachment.setId(resultSet.getLong("id"));
        attachment.setNoteId(resultSet.getLong("note_id"));
        attachment.setStorageName(resultSet.getString("storage_name"));
        attachment.setOriginalFilename(resultSet.getString("original_filename"));
        attachment.setContentType(resultSet.getString("content_type"));
        attachment.setFileSize(resultSet.getLong("file_size"));
        attachment.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
        return attachment;
    }
}
