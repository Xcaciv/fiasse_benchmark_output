package com.loosenotes.servlet;

import com.loosenotes.model.Note;
import com.loosenotes.model.Note.Visibility;
import com.loosenotes.service.NoteService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.CsrfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Optional;

/**
 * CRUD operations for notes (REQ-004, 006, 007, 009).
 * URL patterns:
 *   GET  /notes          → list user's notes
 *   GET  /notes/new      → new note form
 *   POST /notes/new      → create note
 *   GET  /notes/{id}     → view note
 *   GET  /notes/{id}/edit→ edit form
 *   POST /notes/{id}/edit→ update note
 *   POST /notes/{id}/delete → delete note
 */
public final class NoteServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(NoteServlet.class);

    private NoteService noteService;

    @Override
    public void init() {
        this.noteService = (NoteService) getServletContext().getAttribute("noteService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            listNotes(req, resp);
        } else if (pathInfo.equals("/new")) {
            showNewForm(req, resp);
        } else if (pathInfo.matches("/\\d+/edit")) {
            showEditForm(req, resp, extractId(pathInfo));
        } else if (pathInfo.matches("/\\d+")) {
            viewNote(req, resp, extractId(pathInfo));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/new".equals(pathInfo)) {
            createNote(req, resp);
        } else if (pathInfo != null && pathInfo.matches("/\\d+/edit")) {
            updateNote(req, resp, extractId(pathInfo));
        } else if (pathInfo != null && pathInfo.matches("/\\d+/delete")) {
            deleteNote(req, resp, extractId(pathInfo));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void listNotes(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long userId = getSessionUserId(req);
        req.setAttribute("notes", noteService.findByUser(userId));
        req.getRequestDispatcher("/WEB-INF/jsp/note/list.jsp").forward(req, resp);
    }

    private void showNewForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        CsrfUtil.getOrCreateToken(req.getSession());
        req.getRequestDispatcher("/WEB-INF/jsp/note/edit.jsp").forward(req, resp);
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp, long noteId)
            throws ServletException, IOException {
        long userId = getSessionUserId(req);
        Optional<Note> noteOpt = noteService.findById(noteId);
        if (noteOpt.isEmpty() || noteOpt.get().getUserId() != userId) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        req.setAttribute("note", noteOpt.get());
        req.setAttribute("attachments", noteService.getAttachments(noteId));
        req.setAttribute("shareLink", noteService.findShareLink(noteId).orElse(null));
        CsrfUtil.getOrCreateToken(req.getSession());
        req.getRequestDispatcher("/WEB-INF/jsp/note/edit.jsp").forward(req, resp);
    }

    private void viewNote(HttpServletRequest req, HttpServletResponse resp, long noteId)
            throws ServletException, IOException {
        long userId = getSessionUserId(req);
        Optional<Note> noteOpt = noteService.findById(noteId);
        if (noteOpt.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Note note = noteOpt.get();
        // Access control: private notes only visible to owner
        if (!note.isPublic() && note.getUserId() != userId) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        req.setAttribute("note",        note);
        req.setAttribute("attachments", noteService.getAttachments(noteId));
        req.setAttribute("ratings",     noteService.getRatings(noteId));
        req.setAttribute("avgRating",   noteService.getAverageRating(noteId));
        req.setAttribute("ratingCount", noteService.getRatingCount(noteId));
        req.setAttribute("userRating",  noteService.getUserRating(noteId, userId).orElse(null));
        req.setAttribute("shareLink",   noteService.findShareLink(noteId).orElse(null));
        CsrfUtil.getOrCreateToken(req.getSession());
        req.getRequestDispatcher("/WEB-INF/jsp/note/view.jsp").forward(req, resp);
    }

    private void createNote(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long userId  = getSessionUserId(req);
        String title   = req.getParameter("title");
        String content = req.getParameter("content");
        Visibility vis = parseVisibility(req.getParameter("visibility"));
        try {
            Note note = noteService.createNote(userId, title, content, vis);
            resp.sendRedirect(req.getContextPath() + "/notes/" + note.getId());
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/note/edit.jsp").forward(req, resp);
        }
    }

    private void updateNote(HttpServletRequest req, HttpServletResponse resp, long noteId)
            throws ServletException, IOException {
        long userId    = getSessionUserId(req);
        String title   = req.getParameter("title");
        String content = req.getParameter("content");
        Visibility vis = parseVisibility(req.getParameter("visibility"));
        try {
            noteService.updateNote(noteId, userId, title, content, vis);
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/note/edit.jsp").forward(req, resp);
        }
    }

    private void deleteNote(HttpServletRequest req, HttpServletResponse resp, long noteId)
            throws ServletException, IOException {
        long userId    = getSessionUserId(req);
        String role    = (String) req.getSession().getAttribute("userRole");
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        try {
            noteService.deleteNote(noteId, userId, isAdmin);
            resp.sendRedirect(req.getContextPath() + "/notes");
        } catch (ServiceException e) {
            log.warn("Note delete denied: {}", e.getMessage());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    private long getSessionUserId(HttpServletRequest req) {
        return (Long) req.getSession().getAttribute("userId");
    }

    private long extractId(String pathInfo) {
        String[] parts = pathInfo.split("/");
        return Long.parseLong(parts[1]);
    }

    private Visibility parseVisibility(String param) {
        try { return Visibility.valueOf(param.toUpperCase()); }
        catch (Exception e) { return Visibility.PRIVATE; }
    }
}
