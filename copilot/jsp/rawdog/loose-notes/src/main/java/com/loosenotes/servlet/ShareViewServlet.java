package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/share")
public class ShareViewServlet extends HttpServlet {

    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        try {
            ShareLink shareLink = shareLinkDAO.getShareLinkByToken(token);
            if (shareLink == null) {
                req.setAttribute("error", "Invalid or expired share link.");
                req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
                return;
            }
            Note note = noteDAO.getNoteById(shareLink.getNoteId());
            if (note == null) {
                req.setAttribute("error", "Note not found.");
                req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
                return;
            }
            req.setAttribute("note", note);
            req.setAttribute("attachments", attachmentDAO.getAttachmentsByNote(note.getId()));
            req.getRequestDispatcher("/WEB-INF/views/share/view.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
