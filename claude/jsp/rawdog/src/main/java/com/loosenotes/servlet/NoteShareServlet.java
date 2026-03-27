package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.model.User;
import com.loosenotes.util.TokenUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/notes/share")
public class NoteShareServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");
        int id = parseId(req.getParameter("id"));
        if (id <= 0) { resp.sendError(400); return; }

        Note note = noteDAO.findById(id);
        if (note == null) { resp.sendError(404); return; }
        if (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
            resp.sendError(403); return;
        }

        String action = req.getParameter("action");
        if ("revoke".equals(action)) {
            shareLinkDAO.deleteByNoteId(id);
            auditLogDAO.log(currentUser.getId(), "SHARE_REVOKE", "Revoked share link for note id=" + id);
            req.getSession().setAttribute("flash_success", "Share link revoked.");
        } else {
            // Generate new token
            ShareLink existing = shareLinkDAO.findByNoteId(id);
            String token = TokenUtil.generateShareToken();
            if (existing != null) {
                shareLinkDAO.deleteByNoteId(id);
            }
            ShareLink link = new ShareLink();
            link.setNoteId(id);
            link.setToken(token);
            shareLinkDAO.create(link);
            auditLogDAO.log(currentUser.getId(), "SHARE_CREATE", "Created share link for note id=" + id);
            req.getSession().setAttribute("flash_success", "Share link generated.");
        }

        resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + id);
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}
