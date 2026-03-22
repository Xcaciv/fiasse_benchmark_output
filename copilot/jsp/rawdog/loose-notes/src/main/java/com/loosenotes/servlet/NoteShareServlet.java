package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.TokenUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/notes/share")
public class NoteShareServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = (Integer) req.getSession().getAttribute("userId");
        try {
            int noteId = Integer.parseInt(req.getParameter("id"));
            Note note = noteDAO.getNoteById(noteId);
            if (note == null || note.getUserId() != userId) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            ShareLink shareLink = shareLinkDAO.getShareLinkByNote(noteId);
            req.setAttribute("note", note);
            req.setAttribute("shareLink", shareLink);
            req.getRequestDispatcher("/WEB-INF/views/notes/share.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = (Integer) req.getSession().getAttribute("userId");
        try {
            int noteId = Integer.parseInt(req.getParameter("id"));
            Note note = noteDAO.getNoteById(noteId);
            if (note == null || note.getUserId() != userId) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String action = req.getParameter("action");
            if ("generate".equals(action)) {
                ShareLink existing = shareLinkDAO.getShareLinkByNote(noteId);
                String newToken = TokenUtil.generateToken();
                if (existing != null) {
                    shareLinkDAO.updateShareLink(noteId, newToken);
                } else {
                    shareLinkDAO.createShareLink(noteId, newToken);
                }
            } else if ("revoke".equals(action)) {
                shareLinkDAO.deleteShareLinkByNote(noteId);
            }
            resp.sendRedirect(req.getContextPath() + "/notes/share?id=" + noteId);
        } catch (Exception e) {
            req.setAttribute("error", "Error: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
