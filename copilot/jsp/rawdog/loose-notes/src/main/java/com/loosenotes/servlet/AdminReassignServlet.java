package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/admin/reassign")
public class AdminReassignServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int noteId = Integer.parseInt(req.getParameter("noteId"));
            Note note = noteDAO.getNoteById(noteId);
            if (note == null) {
                req.setAttribute("error", "Note not found.");
                req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
                return;
            }
            List<User> users = userDAO.getAllUsers();
            req.setAttribute("note", note);
            req.setAttribute("users", users);
            req.getRequestDispatcher("/WEB-INF/views/admin/reassign.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int adminUserId = (Integer) req.getSession().getAttribute("userId");
        try {
            int noteId = Integer.parseInt(req.getParameter("noteId"));
            int newUserId = Integer.parseInt(req.getParameter("newUserId"));
            noteDAO.reassignNote(noteId, newUserId);
            activityLogDAO.log(adminUserId, "NOTE_REASSIGN", "Note id=" + noteId + " reassigned to userId=" + newUserId);
            req.getSession().setAttribute("successMessage", "Note reassigned successfully.");
            resp.sendRedirect(req.getContextPath() + "/admin/users");
        } catch (Exception e) {
            req.setAttribute("error", "Error reassigning note: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
