package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.FileUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-004, REQ-005 – Create note with optional file attachment.
 * SSEM: Authorization (session check), Input Validation, Secure Storage (UUID filenames).
 */
public class NoteCreateServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NoteCreateServlet.class.getName());
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
        req.getRequestDispatcher("/jsp/notes/create.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long userId = (long) req.getSession().getAttribute("userId");

        String title   = ValidationUtil.truncate(req.getParameter("title"), 255);
        String content = req.getParameter("content");
        boolean isPublic = "true".equals(req.getParameter("isPublic"));

        if (ValidationUtil.isBlank(title) || ValidationUtil.isBlank(content)) {
            req.setAttribute("error", "Title and content are required.");
            req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
            req.getRequestDispatcher("/jsp/notes/create.jsp").forward(req, res);
            return;
        }

        try {
            long noteId = noteDAO.create(userId, title, content, isPublic);

            // ── Optional attachment ────────────────────────────────────────
            Part filePart = null;
            try { filePart = req.getPart("attachment"); } catch (Exception ignored) {}

            if (filePart != null && filePart.getSize() > 0) {
                String origName = extractFilename(filePart);
                String fileError = FileUtil.validateFilename(origName);
                if (fileError == null && filePart.getSize() > FileUtil.MAX_FILE_SIZE) {
                    fileError = "File exceeds 10 MB limit.";
                }
                if (fileError == null) {
                    String stored = FileUtil.store(filePart.getInputStream(), origName);
                    attachmentDAO.create(noteId, origName, stored, filePart.getSize());
                } else {
                    LOGGER.warning("Rejected upload from user " + userId + ": " + fileError);
                }
            }

            res.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating note for user " + userId, e);
            req.setAttribute("error", "Failed to create note. Please try again.");
            req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
            req.getRequestDispatcher("/jsp/notes/create.jsp").forward(req, res);
        }
    }

    private String extractFilename(Part part) {
        String header = part.getHeader("content-disposition");
        if (header == null) return "upload";
        for (String token : header.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return "upload";
    }
}
