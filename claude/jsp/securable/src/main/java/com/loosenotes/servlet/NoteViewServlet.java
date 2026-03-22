package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Note;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-004, REQ-010, REQ-011 – View a note.
 * SSEM: Authorization (visibility check before serving content).
 */
public class NoteViewServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NoteViewServlet.class.getName());
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long noteId = ValidationUtil.parseLong(req.getParameter("id"));
        if (noteId < 0) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        HttpSession session = req.getSession(false);
        long viewerId = (session != null && session.getAttribute("userId") != null)
                ? (long) session.getAttribute("userId") : -1;

        try {
            Note note = noteDAO.findById(noteId);
            if (note == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // ── Access control ────────────────────────────────────────────
            boolean isOwner = note.getUserId() == viewerId;
            if (!note.isPublic() && !isOwner) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // ── Load related data ─────────────────────────────────────────
            req.setAttribute("note", note);
            req.setAttribute("isOwner", isOwner);
            req.setAttribute("attachments", attachmentDAO.findByNoteId(noteId));
            req.setAttribute("ratings", ratingDAO.findByNoteId(noteId));

            if (viewerId > 0) {
                req.setAttribute("userRating", ratingDAO.findByNoteAndUser(noteId, viewerId));
            }

            if (isOwner) {
                req.setAttribute("shareLink", shareLinkDAO.findByNoteId(noteId));
            }

            CsrfUtil.getOrCreateToken(req);
            req.setAttribute(CsrfUtil.SESSION_KEY, session != null
                    ? session.getAttribute(CsrfUtil.SESSION_KEY) : null);

            req.getRequestDispatcher("/jsp/notes/view.jsp").forward(req, res);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading note " + noteId, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
