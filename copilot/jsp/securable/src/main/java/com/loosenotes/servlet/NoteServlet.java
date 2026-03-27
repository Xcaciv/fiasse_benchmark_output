package com.loosenotes.servlet;

import com.loosenotes.dao.*;
import com.loosenotes.model.*;
import com.loosenotes.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Handles all note CRUD operations and share/visibility toggling.
 * URL pattern: /notes/*
 */
@WebServlet("/notes/*")
public class NoteServlet extends HttpServlet {

    private NoteDao noteDao;
    private AttachmentDao attachmentDao;
    private RatingDao ratingDao;
    private ShareLinkDao shareLinkDao;

    @Override
    public void init() {
        DatabaseManager db = DatabaseManager.getInstance();
        this.noteDao = new NoteDao(db);
        this.attachmentDao = new AttachmentDao(db);
        this.ratingDao = new RatingDao(db);
        this.shareLinkDao = new ShareLinkDao(db);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            handleListNotes(req, res);
        } else if (pathInfo.equals("/create")) {
            handleCreateForm(req, res);
        } else {
            handleNoteSubPath(req, res, pathInfo);
        }
    }

    private void handleNoteSubPath(HttpServletRequest req, HttpServletResponse res, String pathInfo)
            throws ServletException, IOException {
        String[] segments = pathInfo.split("/");
        if (segments.length < 2) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long noteId = parseId(segments[1]);
        if (noteId <= 0) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (segments.length == 2) {
            handleViewNote(req, res, noteId);
        } else if (segments.length == 3 && "edit".equals(segments[2])) {
            handleEditForm(req, res, noteId);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        String csrfToken = req.getParameter("csrfToken");
        if (!CsrfUtil.validateToken(session, csrfToken)) {
            AuditLogger.logSecurityEvent("CSRF_VIOLATION", req.getRemoteAddr(),
                "path=" + req.getRequestURI());
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/create")) {
            handleCreateNote(req, res);
            return;
        }

        String[] segments = pathInfo.split("/");
        if (segments.length < 3) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long noteId = parseId(segments[1]);
        if (noteId <= 0) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        routePostAction(req, res, noteId, segments[2]);
    }

    private void routePostAction(HttpServletRequest req, HttpServletResponse res,
                                  long noteId, String action)
            throws ServletException, IOException {
        switch (action) {
            case "edit":          handleEditNote(req, res, noteId); break;
            case "delete":        handleDeleteNote(req, res, noteId); break;
            case "toggle-public": handleTogglePublic(req, res, noteId); break;
            case "generate-share":handleGenerateShare(req, res, noteId); break;
            case "revoke-share":  handleRevokeShare(req, res, noteId); break;
            default:              res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleListNotes(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        try {
            List<Note> notes = noteDao.findByUserId(user.getId());
            req.setAttribute("notes", notes);
            req.setAttribute("csrfToken", CsrfUtil.getTokenFromSession(req.getSession()));
            req.getRequestDispatcher("/WEB-INF/views/notes/list.jsp").forward(req, res);
        } catch (SQLException e) {
            handleError(req, res, "Failed to load notes.", e);
        }
    }

    private void handleCreateForm(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.setAttribute("csrfToken", CsrfUtil.getTokenFromSession(req.getSession()));
        req.getRequestDispatcher("/WEB-INF/views/notes/create.jsp").forward(req, res);
    }

    private void handleCreateNote(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        String title = ValidationUtil.sanitizeString(req.getParameter("title"));
        String content = ValidationUtil.sanitizeString(req.getParameter("content"));
        boolean isPublic = "on".equals(req.getParameter("isPublic"));

        if (!ValidationUtil.isValidTitle(title) || !ValidationUtil.isValidContent(content)) {
            req.setAttribute("error", "Title (1–200 chars) and content (max 50000) are required.");
            req.setAttribute("csrfToken", CsrfUtil.getTokenFromSession(req.getSession()));
            req.getRequestDispatcher("/WEB-INF/views/notes/create.jsp").forward(req, res);
            return;
        }
        try {
            Note note = buildNote(user.getId(), title, content, isPublic);
            noteDao.save(note);
            AuditLogger.logNoteAction("CREATE", user.getId(), user.getUsername(),
                note.getId(), req.getRemoteAddr());
            res.sendRedirect(req.getContextPath() + "/notes");
        } catch (SQLException e) {
            handleError(req, res, "Failed to create note.", e);
        }
    }

    private void handleViewNote(HttpServletRequest req, HttpServletResponse res, long noteId)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        try {
            Note note = findNoteOrNotFound(noteId, res);
            if (note == null) return;
            if (!canViewNote(user, note)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            populateViewAttributes(req, note, user);
            req.getRequestDispatcher("/WEB-INF/views/notes/view.jsp").forward(req, res);
        } catch (SQLException e) {
            handleError(req, res, "Failed to load note.", e);
        }
    }

    private void populateViewAttributes(HttpServletRequest req, Note note, User user)
            throws SQLException {
        req.setAttribute("note", note);
        req.setAttribute("attachments", attachmentDao.findByNoteId(note.getId()));
        List<Rating> ratings = ratingDao.findByNoteId(note.getId());
        req.setAttribute("ratings", ratings);
        req.setAttribute("avgRating", ratingDao.getAverageRating(note.getId()));
        req.setAttribute("userRating",
            ratingDao.findByNoteAndUser(note.getId(), user.getId()).orElse(null));
        List<ShareLink> shareLinks = shareLinkDao.findByNoteId(note.getId());
        req.setAttribute("shareLinks", shareLinks);
        req.setAttribute("csrfToken", CsrfUtil.getTokenFromSession(req.getSession()));
    }

    private void handleEditForm(HttpServletRequest req, HttpServletResponse res, long noteId)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        try {
            Note note = findNoteOrNotFound(noteId, res);
            if (note == null) return;
            if (note.getUserId() != user.getId() && !user.isAdmin()) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            req.setAttribute("note", note);
            req.setAttribute("csrfToken", CsrfUtil.getTokenFromSession(req.getSession()));
            req.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(req, res);
        } catch (SQLException e) {
            handleError(req, res, "Failed to load note for editing.", e);
        }
    }

    private void handleEditNote(HttpServletRequest req, HttpServletResponse res, long noteId)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        String title = ValidationUtil.sanitizeString(req.getParameter("title"));
        String content = ValidationUtil.sanitizeString(req.getParameter("content"));
        boolean isPublic = "on".equals(req.getParameter("isPublic"));

        if (!ValidationUtil.isValidTitle(title) || !ValidationUtil.isValidContent(content)) {
            req.setAttribute("error", "Title (1–200 chars) and content (max 50000) are required.");
            req.setAttribute("csrfToken", CsrfUtil.getTokenFromSession(req.getSession()));
            req.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(req, res);
            return;
        }
        try {
            Note note = findNoteOrNotFound(noteId, res);
            if (note == null) return;
            if (note.getUserId() != user.getId() && !user.isAdmin()) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            note.setTitle(title);
            note.setContent(content);
            note.setPublic(isPublic);
            note.setUpdatedAt(System.currentTimeMillis());
            noteDao.update(note);
            AuditLogger.logNoteAction("EDIT", user.getId(), user.getUsername(),
                noteId, req.getRemoteAddr());
            res.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (SQLException e) {
            handleError(req, res, "Failed to update note.", e);
        }
    }

    private void handleDeleteNote(HttpServletRequest req, HttpServletResponse res, long noteId)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        try {
            Note note = findNoteOrNotFound(noteId, res);
            if (note == null) return;
            if (note.getUserId() != user.getId() && !user.isAdmin()) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            // Attachments are deleted from disk before DB record removal
            deleteAttachmentFiles(req, note.getId());
            noteDao.delete(noteId);
            AuditLogger.logNoteAction("DELETE", user.getId(), user.getUsername(),
                noteId, req.getRemoteAddr());
            res.sendRedirect(req.getContextPath() + "/notes");
        } catch (SQLException e) {
            handleError(req, res, "Failed to delete note.", e);
        }
    }

    private void deleteAttachmentFiles(HttpServletRequest req, long noteId) throws SQLException {
        String uploadDir = FileUtil.getUploadDir(req.getServletContext());
        List<com.loosenotes.model.Attachment> attachments = attachmentDao.findByNoteId(noteId);
        for (com.loosenotes.model.Attachment att : attachments) {
            java.io.File file = new java.io.File(uploadDir, att.getStoredFilename());
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private void handleTogglePublic(HttpServletRequest req, HttpServletResponse res, long noteId)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        try {
            Note note = findNoteOrNotFound(noteId, res);
            if (note == null) return;
            if (note.getUserId() != user.getId()) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            note.setPublic(!note.isPublic());
            note.setUpdatedAt(System.currentTimeMillis());
            noteDao.update(note);
            AuditLogger.logNoteAction("TOGGLE_PUBLIC", user.getId(), user.getUsername(),
                noteId, req.getRemoteAddr());
            res.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (SQLException e) {
            handleError(req, res, "Failed to update note visibility.", e);
        }
    }

    private void handleGenerateShare(HttpServletRequest req, HttpServletResponse res, long noteId)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        try {
            Note note = findNoteOrNotFound(noteId, res);
            if (note == null) return;
            if (note.getUserId() != user.getId()) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            ShareLink shareLink = new ShareLink();
            shareLink.setNoteId(noteId);
            shareLink.setToken(TokenUtil.generateShareToken());
            shareLink.setCreatedAt(System.currentTimeMillis());
            shareLinkDao.save(shareLink);
            AuditLogger.logNoteAction("SHARE", user.getId(), user.getUsername(),
                noteId, req.getRemoteAddr());
            res.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (SQLException e) {
            handleError(req, res, "Failed to generate share link.", e);
        }
    }

    private void handleRevokeShare(HttpServletRequest req, HttpServletResponse res, long noteId)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        try {
            Note note = findNoteOrNotFound(noteId, res);
            if (note == null) return;
            if (note.getUserId() != user.getId()) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            shareLinkDao.deleteByNoteId(noteId);
            AuditLogger.logNoteAction("REVOKE_SHARE", user.getId(), user.getUsername(),
                noteId, req.getRemoteAddr());
            res.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (SQLException e) {
            handleError(req, res, "Failed to revoke share links.", e);
        }
    }

    private boolean canViewNote(User user, Note note) {
        return note.getUserId() == user.getId() || note.isPublic() || user.isAdmin();
    }

    private Note findNoteOrNotFound(long noteId, HttpServletResponse res)
            throws SQLException, IOException {
        Optional<Note> opt = noteDao.findById(noteId);
        if (opt.isEmpty()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return opt.get();
    }

    private Note buildNote(long userId, String title, String content, boolean isPublic) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(title);
        note.setContent(content);
        note.setPublic(isPublic);
        long now = System.currentTimeMillis();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        return note;
    }

    private User getCurrentUser(HttpServletRequest req) {
        return (User) req.getSession(false).getAttribute("currentUser");
    }

    private long parseId(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private void handleError(HttpServletRequest req, HttpServletResponse res,
                              String message, Exception e) throws ServletException, IOException {
        getServletContext().log(message, e);
        req.setAttribute("errorMessage", message);
        req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, res);
    }
}
