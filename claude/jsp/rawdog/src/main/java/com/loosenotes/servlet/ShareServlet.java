package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ShareServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ShareServlet.class);
    private final NoteDAO noteDAO = new NoteDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.sendRedirect(req.getContextPath() + "/notes");
            return;
        }

        String[] parts = pathInfo.split("/");
        if (parts.length < 2) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String token = parts[1];
        ShareLink shareLink = shareLinkDAO.findByToken(token);
        if (shareLink == null) {
            req.setAttribute("errorMessage", "This share link is invalid or has been revoked.");
            req.getRequestDispatcher("/WEB-INF/views/shared/error.jsp").forward(req, resp);
            return;
        }

        Note note = noteDAO.findById(shareLink.getNoteId());
        if (note == null) {
            req.setAttribute("errorMessage", "The note associated with this link no longer exists.");
            req.getRequestDispatcher("/WEB-INF/views/shared/error.jsp").forward(req, resp);
            return;
        }

        List<Attachment> attachments = attachmentDAO.findByNoteId(note.getId());
        req.setAttribute("note", note);
        req.setAttribute("attachments", attachments);
        req.setAttribute("shareToken", token);
        req.getRequestDispatcher("/WEB-INF/views/share/view.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            resp.sendRedirect(req.getContextPath() + "/notes");
            return;
        }

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // POST /share/generate?noteId=X or /share/revoke?noteId=X
        String[] parts = pathInfo.split("/");
        if (parts.length < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String action = parts[1]; // "generate" or "revoke"
        String noteIdStr = req.getParameter("noteId");
        if (noteIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int noteId = Integer.parseInt(noteIdStr);
            Note note = noteDAO.findById(noteId);
            if (note == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if ("generate".equals(action)) {
                String newToken = UUID.randomUUID().toString().replace("-", "");
                ShareLink existing = shareLinkDAO.findByNoteId(noteId);
                if (existing != null) {
                    shareLinkDAO.updateToken(noteId, newToken);
                } else {
                    ShareLink link = new ShareLink();
                    link.setNoteId(noteId);
                    link.setToken(newToken);
                    shareLinkDAO.create(link);
                }
                logger.info("Share link generated for note {} by user {}", noteId, currentUser.getUsername());
            } else if ("revoke".equals(action)) {
                shareLinkDAO.deleteByNoteId(noteId);
                logger.info("Share link revoked for note {} by user {}", noteId, currentUser.getUsername());
            }

            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
