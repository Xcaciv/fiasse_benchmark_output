package com.loosenotes.servlet;

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

@WebServlet("/admin/users")
public class AdminUsersServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<User> users = userDAO.findAll();
        req.setAttribute("users", users);

        // If userId is provided, also show notes for that user
        String userIdStr = req.getParameter("userId");
        if (userIdStr != null) {
            try {
                int userId = Integer.parseInt(userIdStr);
                User selectedUser = userDAO.findById(userId);
                if (selectedUser != null) {
                    List<Note> notes = noteDAO.findByUserId(userId);
                    req.setAttribute("selectedUser", selectedUser);
                    req.setAttribute("notes", notes);
                }
            } catch (NumberFormatException ignored) {}
        }

        req.getRequestDispatcher("/WEB-INF/jsp/admin/users.jsp").forward(req, resp);
    }
}
