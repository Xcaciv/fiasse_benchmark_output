package com.loosenotes.servlet;

import com.loosenotes.model.Note;
import com.loosenotes.model.Note.Visibility;
import com.loosenotes.model.User;
import com.loosenotes.service.AttachmentService;
import com.loosenotes.service.RatingService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.service.ShareLinkService;
import com.loosenotes.util.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Handles note CRUD, search, and top-rated pages.
 * URL patterns:
 *   GET  /notes            – list owned notes
 *   GET  /notes/{id}       – view a note
 *   GET  /notes/create     – show create form
 *   POST /notes/create     – create note
 *   GET  /notes/{id}/edit  – show edit form
 *   POST /notes/{id}/edit  – update note
 *   POST /notes/{id}/delete – delete note
 *   GET  /notes/top-rated  – top-rated public notes
 *
 * SSEM notes:
 * - Integrity: Derived Integrity – visibility parsed from allowed values only.
 * - Accountability: all mutations audited by NoteService.
 */
@MultipartConfig(maxFileSize = 5_242_880, maxRequestSize = 10_485_760)
public class NoteServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getRequiredUser(req, resp);
        if (user == null) return;

        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();

        if (path.equals("/") || path.isEmpty()) {
            listNotes(req, resp, user);
        } else if (path.equals("/create")) {
            forward(req, resp, "notes/create.jsp");
        } else if (path.equals("/top-rated")) {
            showTopRated(req, resp);
        } else if (path.endsWith("/edit")) {
            showEditForm(req, resp, user);
        } else {
            showNoteDetail(req, resp, user);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getRequiredUser(req, resp);
        if (user == null) return;

        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();

        if (path.equals("/create")) {
            createNote(req, resp, user);
        } else if (path.endsWith("/edit")) {
            updateNote(req, resp, user);
        } else if (path.endsWith("/delete")) {
            deleteNote(req, resp, user);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void listNotes(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        try {
            List<Note> notes = getNoteService().getNotesForOwner(user.getId());
            req.setAttribute("notes", notes);
            forward(req, resp, "notes/index.jsp");
        } catch (SQLException e) {
            log.error("Error listing notes", e);
            sendError(req, resp, 500, "Could not load notes");
        }
    }

    private void showNoteDetail(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        long noteId = parseIdFromPath(req);
        if (noteId < 0) { resp.sendError(404); return; }
        try {
            Note note = getNoteService().getNoteForUser(noteId, user.getId(), user.isAdmin());
            List<?> attachments = getAttachmentService()
                    .getAttachmentsForNote(noteId, user.getId(), user.isAdmin());
            List<?> ratings = getRatingService().getRatingsForNote(noteId);
            var existingRating = getRatingService().getExistingRating(noteId, user.getId());
            var shareLink = getShareLinkService().getLinkForNote(noteId, user.getId());

            req.setAttribute("note", note);
            req.setAttribute("attachments", attachments);
            req.setAttribute("ratings", ratings);
            req.setAttribute("existingRating", existingRating.orElse(null));
            req.setAttribute("shareLink", shareLink.orElse(null));
            forward(req, resp, "notes/view.jsp");
        } catch (ServiceException e) {
            sendError(req, resp, 403, e.getMessage());
        } catch (SQLException e) {
            log.error("Error loading note {}", noteId, e);
            sendError(req, resp, 500, "Could not load note");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        long noteId = parseIdFromPath(req);
        if (noteId < 0) { resp.sendError(404); return; }
        try {
            Note note = getNoteService().getNoteForUser(noteId, user.getId(), user.isAdmin());
            req.setAttribute("note", note);
            forward(req, resp, "notes/edit.jsp");
        } catch (ServiceException e) {
            sendError(req, resp, 403, e.getMessage());
        } catch (SQLException e) {
            log.error("Error loading note for edit {}", noteId, e);
            sendError(req, resp, 500, "Could not load note");
        }
    }

    private void showTopRated(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setAttribute("notes", getNoteService().getTopRated());
            forward(req, resp, "notes/topRated.jsp");
        } catch (SQLException e) {
            log.error("Error loading top-rated notes", e);
            sendError(req, resp, 500, "Could not load top-rated notes");
        }
    }

    private void createNote(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        // Trust boundary: sanitize all inputs
        String title      = InputSanitizer.sanitizeLine(req.getParameter("title"));
        String content    = InputSanitizer.sanitizeMultiline(req.getParameter("content"));
        Visibility vis    = parseVisibility(req.getParameter("visibility"));

        try {
            long noteId = getNoteService().createNote(
                    user.getId(), title, content, vis, getClientIp(req));

            // Handle optional file attachment
            Part filePart = getFilePart(req);
            if (filePart != null && filePart.getSize() > 0) {
                getAttachmentService().saveAttachment(
                        noteId, filePart, user.getId(), getClientIp(req));
            }
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("title", title);
            req.setAttribute("content", content);
            forward(req, resp, "notes/create.jsp");
        } catch (SQLException e) {
            log.error("Error creating note", e);
            req.setAttribute("error", "A system error occurred");
            forward(req, resp, "notes/create.jsp");
        }
    }

    private void updateNote(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        long noteId   = parseIdFromPath(req);
        if (noteId < 0) { resp.sendError(404); return; }

        String title   = InputSanitizer.sanitizeLine(req.getParameter("title"));
        String content = InputSanitizer.sanitizeMultiline(req.getParameter("content"));
        Visibility vis = parseVisibility(req.getParameter("visibility"));

        try {
            getNoteService().updateNote(noteId, user.getId(), user.isAdmin(),
                    title, content, vis, getClientIp(req));
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException e) {
            try {
                Note note = getNoteService().getNoteForUser(noteId, user.getId(), user.isAdmin());
                req.setAttribute("note", note);
            } catch (Exception ignored) {}
            req.setAttribute("error", e.getMessage());
            forward(req, resp, "notes/edit.jsp");
        } catch (SQLException e) {
            log.error("Error updating note {}", noteId, e);
            sendError(req, resp, 500, "Could not update note");
        }
    }

    private void deleteNote(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        long noteId = parseIdFromPath(req);
        if (noteId < 0) { resp.sendError(404); return; }

        try {
            getNoteService().deleteNote(noteId, user.getId(), user.isAdmin(), getClientIp(req));
            resp.sendRedirect(req.getContextPath() + "/notes");
        } catch (ServiceException e) {
            sendError(req, resp, 403, e.getMessage());
        } catch (SQLException e) {
            log.error("Error deleting note {}", noteId, e);
            sendError(req, resp, 500, "Could not delete note");
        }
    }

    /** Parses visibility from form parameter; defaults to PRIVATE on unknown input. */
    private Visibility parseVisibility(String value) {
        try {
            return Visibility.valueOf(value != null ? value.toUpperCase() : "PRIVATE");
        } catch (IllegalArgumentException e) {
            return Visibility.PRIVATE;
        }
    }

    private Part getFilePart(HttpServletRequest req) {
        try {
            return req.getPart("attachment");
        } catch (Exception e) {
            return null;
        }
    }
}
