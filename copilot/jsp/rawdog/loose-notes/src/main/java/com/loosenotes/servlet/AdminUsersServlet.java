package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/admin/users")
public class AdminUsersServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<User> users = userDAO.getAllUsers();
            String search = req.getParameter("search");
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                users.removeIf(u -> !u.getUsername().toLowerCase().contains(searchLower) &&
                                    !u.getEmail().toLowerCase().contains(searchLower));
                req.setAttribute("search", search);
            }
            req.setAttribute("users", users);
            req.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error loading users: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
