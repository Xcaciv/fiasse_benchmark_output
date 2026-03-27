package com.loosenotes.servlet;

import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.ShareLink;
import com.loosenotes.model.User;
import com.loosenotes.service.NoteService;
import com.loosenotes.service.RatingService;
import com.loosenotes.service.ShareLinkService;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Handles CRUD operations for notes.
 * SSEM: Integrity - ownership verified on all mutations.
 * SSEM: Accountability - mutations logged.
 */
@WebServlet("/notes/*")
public class NoteServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(NoteServlet.class);

    private static final String LIST_JSP    = "/WEB-INF/jsp/notes/index.jsp";
    private static final String CREATE_JSP  = "/WEB-INF/jsp/notes/create.jsp";
    private static final String EDIT_JSP    = "/WEB-INF/jsp/notes/edit.jsp";
    private static final String VIEW_JSP    = "/WEB-INF/jsp/notes/view.jsp";
    private static final String DELETE_JSP  = "/WEB-INF/jsp/notes/delete.jsp";
    private static final String TOP_JSP     = "/WEB-INF/jsp/notes/topRated.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String action = getAction(req);
        switch (action) {
            case ""        -> showList(req, res);
            case "create"  -> forward(req, res, CREATE_JSP);
            case "top-rated" -> showTopRated(req, res);
            case "view"    -> showView(req, res);
            case "edit"    -> showEdit(req, res);
            case "delete"  -> showDelete(req, res);
            default        -> sendNotFound(res);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String action = getAction(req);
        switch (action) {
            case "create" -> handleCreate(req, res);
            case "edit"   -> handleEdit(req, res);
            case "delete" -> handleDelete(req, res);
            case "share"  -> handleShare(req, res);
            case "revoke-share" -> handleRevokeShare(req, res);
            default       -> sendNotFound(res);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        try {
            List<Note> notes = getNoteService().getUserNotes(user.getId());
            req.setAttribute("notes", notes);
            forward(req, res, LIST_JSP);
        } catch (SQLException e) {
            log.error("Error loading notes list", e);
            forwardWithError(req, res, LIST_JSP, "Could not load notes.");
        }
    }

    private void showTopRated(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            List<Note> notes = getNoteService().getTopRated(3, 20);
            req.setAttribute("notes", notes);
            forward(req, res, TOP_JSP);
        } catch (SQLException e) {
            log.error("Error loading top rated notes", e);
            forwardWithError(req, res, TOP_JSP, "Could not load top rated notes.");
        }
    }

    private void showView(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        long noteId = parseNoteId(req, res);
        if (noteId < 0) return;
        User user = getCurrentUser(req);
        try {
            Optional<Note> note = getNoteService().getNote(noteId, user.getId());
            if (note.isEmpty()) { sendNotFound(res); return; }

            List<Rating> ratings = getRatingService().getRatings(noteId);
            Optional<Rating> userRating = getRatingService().getUserRating(noteId, user.getId());
            Optional<ShareLink> shareLink = getShareLinkService().getActiveLinkForNote(noteId);

            req.setAttribute("note", note.get());
            req.setAttribute("ratings", ratings);
            req.setAttribute("userRating", userRating.orElse(null));
            req.setAttribute("shareLink", shareLink.orElse(null));
            req.setAttribute("appConfig", getAppConfig());
            forward(req, res, VIEW_JSP);
        } catch (SQLException e) {
            log.error("Error loading note id={}", noteId, e);
            sendNotFound(res);
        }
    }

    private void showEdit(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        long noteId = parseNoteId(req, res);
        if (noteId < 0) return;
        User user = getCurrentUser(req);
        try {
            Optional<Note> note = getNoteService().getNote(noteId, user.getId());
            if (note.isEmpty() || note.get().getUserId() != user.getId()) {
                sendForbidden(res); return;
            }
            req.setAttribute("note", note.get());
            forward(req, res, EDIT_JSP);
        } catch (SQLException e) {
            log.error("Error loading note for edit id={}", noteId, e);
            sendNotFound(res);
        }
    }

    private void showDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        long noteId = parseNoteId(req, res);
        if (noteId < 0) return;
        User user = getCurrentUser(req);
        try {
            Optional<Note> note = getNoteService().getNote(noteId, user.getId());
            if (note.isEmpty()) { sendNotFound(res); return; }
            boolean isOwner = note.get().getUserId() == user.getId();
            if (!isOwner && !user.isAdmin()) { sendForbidden(res); return; }
            req.setAttribute("note", note.get());
            forward(req, res, DELETE_JSP);
        } catch (SQLException e) {
            log.error("Error loading note for delete id={}", noteId, e);
            sendNotFound(res);
        }
    }

    private void handleCreate(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        String title    = req.getParameter("title");
        String content  = req.getParameter("content");
        boolean isPublic = "true".equals(req.getParameter("isPublic"));
        try {
            long id = getNoteService().createNote(user.getId(), title, content, isPublic);
            getAuditService().logNoteCreated(user.getId(), id, getClientIp(req));
            redirect(res, req, "/notes/view/" + id);
        } catch (IllegalArgumentException e) {
            req.setAttribute("title", title);
            req.setAttribute("content", content);
            forwardWithError(req, res, CREATE_JSP, e.getMessage());
        } catch (SQLException e) {
            log.error("Error creating note", e);
            forwardWithError(req, res, CREATE_JSP, "Could not create note.");
        }
    }

    private void handleEdit(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        long noteId = parseNoteId(req, res);
        if (noteId < 0) return;
        User user    = getCurrentUser(req);
        String title   = req.getParameter("title");
        String content = req.getParameter("content");
        boolean isPublic = "true".equals(req.getParameter("isPublic"));
        try {
            boolean updated = getNoteService().updateNote(noteId, user.getId(), title, content, isPublic);
            if (!updated) { sendForbidden(res); return; }
            getAuditService().logNoteUpdated(user.getId(), noteId, getClientIp(req));
            redirect(res, req, "/notes/view/" + noteId);
        } catch (IllegalArgumentException e) {
            req.setAttribute("noteId", noteId);
            forwardWithError(req, res, EDIT_JSP, e.getMessage());
        } catch (SQLException e) {
            log.error("Error updating note id={}", noteId, e);
            forwardWithError(req, res, EDIT_JSP, "Could not update note.");
        }
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        long noteId = parseNoteId(req, res);
        if (noteId < 0) return;
        User user = getCurrentUser(req);
        try {
            boolean deleted = getNoteService().deleteNote(noteId, user.getId(), user.isAdmin());
            if (!deleted) { sendForbidden(res); return; }
            getAuditService().logNoteDeleted(user.getId(), noteId, getClientIp(req));
            redirect(res, req, "/notes");
        } catch (SQLException e) {
            log.error("Error deleting note id={}", noteId, e);
            forwardWithError(req, res, DELETE_JSP, "Could not delete note.");
        }
    }

    private void handleShare(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        long noteId = parseNoteId(req, res);
        if (noteId < 0) return;
        User user = getCurrentUser(req);
        try {
            Optional<String> token = getShareLinkService().generateLink(noteId, user.getId());
            if (token.isEmpty()) { sendForbidden(res); return; }
            redirect(res, req, "/notes/view/" + noteId + "?shared=true");
        } catch (SQLException e) {
            log.error("Error generating share link for note id={}", noteId, e);
            redirect(res, req, "/notes/view/" + noteId + "?shareError=true");
        }
    }

    private void handleRevokeShare(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        long noteId = parseNoteId(req, res);
        if (noteId < 0) return;
        User user = getCurrentUser(req);
        try {
            getShareLinkService().revokeLinks(noteId, user.getId());
            redirect(res, req, "/notes/view/" + noteId + "?revoked=true");
        } catch (SecurityException e) {
            sendForbidden(res);
        } catch (SQLException e) {
            log.error("Error revoking share link for note id={}", noteId, e);
            redirect(res, req, "/notes/view/" + noteId);
        }
    }

    private String getAction(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) return "";
        String[] parts = pathInfo.substring(1).split("/");
        // Paths: /notes/create | /notes/view/123 | /notes/edit/123 etc.
        return parts[0];
    }

    private long parseNoteId(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) { sendNotFound(res); return -1; }
        String[] parts = pathInfo.substring(1).split("/");
        if (parts.length < 2) { sendNotFound(res); return -1; }
        long id = ValidationUtil.parseLongSafe(parts[1]);
        if (!ValidationUtil.isValidId(id)) { sendNotFound(res); return -1; }
        return id;
    }
}
