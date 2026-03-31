package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@WebServlet("/notes")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,
    maxFileSize = 1024 * 1024 * 10,
    maxRequestSize = 1024 * 1024 * 20
)
public class NoteServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(NoteServlet.class);
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"
    );
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            switch (action == null ? "list" : action) {
                case "new":
                    showNewNoteForm(request, response);
                    break;
                case "edit":
                    showEditNoteForm(request, response, user);
                    break;
                case "view":
                    viewNote(request, response, user);
                    break;
                case "delete":
                    deleteNote(request, response, user);
                    break;
                case "search":
                    searchNotes(request, response, user);
                    break;
                default:
                    listNotes(request, response, user);
            }
        } catch (SQLException e) {
            logger.error("Database error in NoteServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            switch (action == null ? "" : action) {
                case "create":
                    createNote(request, response, user);
                    break;
                case "update":
                    updateNote(request, response, user);
                    break;
                case "uploadAttachment":
                    uploadAttachment(request, response, user);
                    break;
                case "deleteAttachment":
                    deleteAttachment(request, response, user);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/notes");
            }
        } catch (SQLException e) {
            logger.error("Database error in NoteServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }
    
    private void listNotes(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        List<Note> notes = noteDAO.findByUserId(user.getId());
        request.setAttribute("notes", notes);
        request.getRequestDispatcher("/WEB-INF/views/notes/list.jsp").forward(request, response);
    }
    
    private void showNewNoteForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/notes/new.jsp").forward(request, response);
    }
    
    private void showEditNoteForm(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        Long noteId = getNoteIdFromRequest(request);
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        Note note = noteDAO.findById(noteId).orElse(null);
        if (note == null || (!note.getUserId().equals(user.getId()) && !user.isAdmin())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        request.setAttribute("note", note);
        request.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(request, response);
    }
    
    private void viewNote(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        Long noteId = getNoteIdFromRequest(request);
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        Note note = noteDAO.findById(noteId).orElse(null);
        if (note == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Note not found");
            return;
        }
        
        boolean hasAccess = note.getUserId().equals(user.getId()) || 
                           note.isPublic() || 
                           user.isAdmin();
        
        if (!hasAccess) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        note.setAttachments(attachmentDAO.findByNoteId(noteId));
        note.setRatings(ratingDAO.findByNoteId(noteId));
        note.setAverageRating(ratingDAO.getAverageRating(noteId));
        
        request.setAttribute("note", note);
        request.getRequestDispatcher("/WEB-INF/views/notes/view.jsp").forward(request, response);
    }
    
    private void deleteNote(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        Long noteId = getNoteIdFromRequest(request);
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        Note note = noteDAO.findById(noteId).orElse(null);
        if (note == null || (!note.getUserId().equals(user.getId()) && !user.isAdmin())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        List<com.loosenotes.model.Attachment> attachments = attachmentDAO.findByNoteId(noteId);
        for (com.loosenotes.model.Attachment attachment : attachments) {
            deleteFile(attachment.getStoredFilename());
        }
        
        noteDAO.delete(noteId);
        activityLogDAO.log(user.getId(), "NOTE_DELETED", "Note ID: " + noteId, getClientIp(request));
        logger.info("Note deleted: {} by user: {}", noteId, user.getUsername());
        
        response.sendRedirect(request.getContextPath() + "/notes");
    }
    
    private void searchNotes(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        String keyword = request.getParameter("keyword");
        
        if (keyword == null || keyword.trim().isEmpty()) {
            listNotes(request, response, user);
            return;
        }
        
        List<Note> notes = noteDAO.search(keyword.trim(), user.getId());
        request.setAttribute("notes", notes);
        request.setAttribute("keyword", keyword);
        request.getRequestDispatcher("/WEB-INF/views/notes/search.jsp").forward(request, response);
    }
    
    private void createNote(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException, ServletException {
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        boolean isPublic = "on".equals(request.getParameter("isPublic"));
        
        if (title == null || title.trim().isEmpty()) {
            request.setAttribute("error", "Title is required");
            request.getRequestDispatcher("/WEB-INF/views/notes/new.jsp").forward(request, response);
            return;
        }
        
        if (content == null || content.trim().isEmpty()) {
            request.setAttribute("error", "Content is required");
            request.getRequestDispatcher("/WEB-INF/views/notes/new.jsp").forward(request, response);
            return;
        }
        
        Note note = new Note(user.getId(), title.trim(), content.trim());
        note.setPublic(isPublic);
        
        Long noteId = noteDAO.create(note);
        
        if (noteId != null) {
            activityLogDAO.log(user.getId(), "NOTE_CREATED", "Note ID: " + noteId, getClientIp(request));
            logger.info("Note created: {} by user: {}", noteId, user.getUsername());
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
        } else {
            request.setAttribute("error", "Failed to create note");
            request.getRequestDispatcher("/WEB-INF/views/notes/new.jsp").forward(request, response);
        }
    }
    
    private void updateNote(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException, ServletException {
        Long noteId = getNoteIdFromRequest(request);
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        Note note = noteDAO.findById(noteId).orElse(null);
        if (note == null || (!note.getUserId().equals(user.getId()) && !user.isAdmin())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        boolean isPublic = "on".equals(request.getParameter("isPublic"));
        
        if (title == null || title.trim().isEmpty()) {
            request.setAttribute("error", "Title is required");
            request.setAttribute("note", note);
            request.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(request, response);
            return;
        }
        
        if (content == null || content.trim().isEmpty()) {
            request.setAttribute("error", "Content is required");
            request.setAttribute("note", note);
            request.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(request, response);
            return;
        }
        
        note.setTitle(title.trim());
        note.setContent(content.trim());
        note.setPublic(isPublic);
        
        noteDAO.update(note);
        activityLogDAO.log(user.getId(), "NOTE_UPDATED", "Note ID: " + noteId, getClientIp(request));
        logger.info("Note updated: {} by user: {}", noteId, user.getUsername());
        
        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
    }
    
    private void uploadAttachment(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException, ServletException {
        Long noteId = getNoteIdFromRequest(request);
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        Note note = noteDAO.findById(noteId).orElse(null);
        if (note == null || (!note.getUserId().equals(user.getId()) && !user.isAdmin())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSubmittedFileName() == null || filePart.getSubmittedFileName().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            return;
        }
        
        String originalFilename = filePart.getSubmittedFileName();
        String extension = getFileExtension(originalFilename).toLowerCase();
        
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            request.setAttribute("error", "File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
            request.setAttribute("note", note);
            request.getRequestDispatcher("/WEB-INF/views/notes/view.jsp").forward(request, response);
            return;
        }
        
        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        String contentType = filePart.getContentType();
        long fileSize = filePart.getSize();
        
        String uploadPath = getServletContext().getRealPath("/uploads");
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        Path filePath = Paths.get(uploadPath, storedFilename);
        
        try (InputStream input = filePart.getInputStream()) {
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        com.loosenotes.model.Attachment attachment = new com.loosenotes.model.Attachment(
            noteId, originalFilename, storedFilename, contentType, fileSize
        );
        
        attachmentDAO.create(attachment);
        activityLogDAO.log(user.getId(), "ATTACHMENT_UPLOADED", "Note ID: " + noteId + ", File: " + originalFilename, getClientIp(request));
        
        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
    }
    
    private void deleteAttachment(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        Long attachmentId = getAttachmentIdFromRequest(request);
        Long noteId = getNoteIdFromRequest(request);
        
        if (attachmentId == null || noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        com.loosenotes.model.Attachment attachment = attachmentDAO.findById(attachmentId).orElse(null);
        if (attachment == null) {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            return;
        }
        
        Note note = noteDAO.findById(noteId).orElse(null);
        if (note == null || (!note.getUserId().equals(user.getId()) && !user.isAdmin())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        deleteFile(attachment.getStoredFilename());
        attachmentDAO.delete(attachmentId);
        
        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
    }
    
    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }
    
    private Long getNoteIdFromRequest(HttpServletRequest request) {
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                return Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Long getAttachmentIdFromRequest(HttpServletRequest request) {
        String idParam = request.getParameter("attachmentId");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                return Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }
    
    private void deleteFile(String storedFilename) {
        try {
            Path filePath = Paths.get(getServletContext().getRealPath("/uploads"), storedFilename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", storedFilename, e);
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
