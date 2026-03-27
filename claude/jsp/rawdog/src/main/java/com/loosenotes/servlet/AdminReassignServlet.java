package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/admin/reassign")
public class AdminReassignServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentAdmin = (User) req.getSession().getAttribute("currentUser");
        int noteId = parseId(req.getParameter("noteId"));
        if (noteId <= 0) { resp.sendError(400); return; }

        Note note = noteDAO.findById(noteId);
        if (note == null) { resp.sendError(404); return; }

        List<User> users = userDAO.findAll();
        req.setAttribute("note", note);
        req.setAttribute("users", users);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/reassign.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentAdmin = (User) req.getSession().getAttribute("currentUser");
        int noteId = parseId(req.getParameter("noteId"));
        int newUserId = parseId(req.getParameter("newUserId"));

        if (noteId <= 0 || newUserId <= 0) { resp.sendError(400); return; }

        Note note = noteDAO.findById(noteId);
        if (note == null) { resp.sendError(404); return; }

        User newOwner = userDAO.findById(newUserId);
        if (newOwner == null) { resp.sendError(404, "Target user not found."); return; }

        if (noteDAO.reassignOwner(noteId, newUserId)) {
            auditLogDAO.log(currentAdmin.getId(), "NOTE_REASSIGN",
                "Reassigned note id=" + noteId + " to user id=" + newUserId);
            req.getSession().setAttribute("flash_success", "Note reassigned to " + newOwner.getUsername() + ".");
        } else {
            req.getSession().setAttribute("flash_error", "Failed to reassign note.");
        }
        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}
