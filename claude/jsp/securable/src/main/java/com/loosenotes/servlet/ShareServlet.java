package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-008 – Share link management.
 *
 * GET  /share/{token}          – public view via share link (no auth required)
 * POST /share/generate         – create/regenerate share link (owner only)
 * POST /share/revoke           – revoke share link (owner only)
 *
 * SSEM: Authorization (owner check for management), Cryptography (SecureRandom token).
 */
public class ShareServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ShareServlet.class.getName());
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = req.getPathInfo(); // "/{token}"
        if (path == null || path.length() < 2) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String token = path.substring(1);

        try {
            ShareLink sl = shareLinkDAO.findByToken(token);
            if (sl == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

            Note note = noteDAO.findById(sl.getNoteId());
            if (note == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

            req.setAttribute("note", note);
            req.setAttribute("attachments", attachmentDAO.findByNoteId(note.getId()));
            req.setAttribute("ratings", ratingDAO.findByNoteId(note.getId()));
            req.setAttribute("shareToken", token);

            req.getRequestDispatcher("/jsp/notes/view.jsp").forward(req, res);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading shared note", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long userId = (long) req.getSession().getAttribute("userId");
        String action = req.getPathInfo(); // "/generate" or "/revoke"

        long noteId = ValidationUtil.parseLong(req.getParameter("noteId"));
        if (noteId < 0) { res.sendError(HttpServletResponse.SC_BAD_REQUEST); return; }

        try {
            Note note = noteDAO.findById(noteId);
            if (note == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            if (note.getUserId() != userId) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

            if ("/generate".equals(action)) {
                String token = shareLinkDAO.createOrReplace(noteId);
                LOGGER.info("Share link generated for note " + noteId + " by user " + userId);
                res.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);

            } else if ("/revoke".equals(action)) {
                shareLinkDAO.deleteByNoteId(noteId);
                LOGGER.info("Share link revoked for note " + noteId + " by user " + userId);
                res.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);

            } else {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Share management error for note " + noteId, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
