package com.loosenotes.servlet;

import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
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
 * Share link management (REQ-008).
 * GET  /share/{token}          → public view (no auth required)
 * POST /share/generate?noteId  → generate/regenerate share link
 * POST /share/revoke?noteId    → revoke share link
 */
public final class ShareServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ShareServlet.class);

    private NoteService noteService;

    @Override
    public void init() {
        this.noteService = (NoteService) getServletContext().getAttribute("noteService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            String token = pathInfo.substring(1);
            viewShared(req, resp, token);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        long noteId = parseLong(req.getParameter("noteId"), -1);
        if (noteId < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid noteId");
            return;
        }
        long userId = (Long) req.getSession().getAttribute("userId");

        if ("/generate".equals(pathInfo)) {
            generateLink(req, resp, noteId, userId);
        } else if ("/revoke".equals(pathInfo)) {
            revokeLink(req, resp, noteId, userId);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /** Public view: no authentication required; only shows note data. */
    private void viewShared(HttpServletRequest req, HttpServletResponse resp, String token)
            throws ServletException, IOException {
        Optional<Note> noteOpt = noteService.findNoteByShareToken(token);
        if (noteOpt.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Share link not found or expired");
            return;
        }
        req.setAttribute("note",        noteOpt.get());
        req.setAttribute("attachments", noteService.getAttachments(noteOpt.get().getId()));
        req.setAttribute("ratings",     noteService.getRatings(noteOpt.get().getId()));
        req.setAttribute("avgRating",   noteService.getAverageRating(noteOpt.get().getId()));
        req.getRequestDispatcher("/WEB-INF/jsp/note/share-view.jsp").forward(req, resp);
    }

    private void generateLink(HttpServletRequest req, HttpServletResponse resp,
                               long noteId, long userId) throws IOException {
        try {
            ShareLink link = noteService.generateShareLink(noteId, userId);
            log.info("Share link generated for note {} by user {}", noteId, userId);
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId + "/edit?shared=1");
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    private void revokeLink(HttpServletRequest req, HttpServletResponse resp,
                             long noteId, long userId) throws IOException {
        try {
            noteService.revokeShareLink(noteId, userId);
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId + "/edit?revoked=1");
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    private long parseLong(String s, long defaultVal) {
        if (s == null) return defaultVal;
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return defaultVal; }
    }
}
