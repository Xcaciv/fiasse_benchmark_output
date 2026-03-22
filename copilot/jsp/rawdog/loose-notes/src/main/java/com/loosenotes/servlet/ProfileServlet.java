package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = (Integer) req.getSession().getAttribute("userId");
        try {
            User user = userDAO.findById(userId);
            req.setAttribute("user", user);
            req.getRequestDispatcher("/WEB-INF/views/profile/edit.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error loading profile: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = (Integer) req.getSession().getAttribute("userId");
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String currentPassword = req.getParameter("currentPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");

        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // Check username uniqueness
            User existingByUsername = userDAO.findByUsername(username);
            if (existingByUsername != null && existingByUsername.getId() != userId) {
                req.setAttribute("error", "Username already taken.");
                req.setAttribute("user", user);
                req.getRequestDispatcher("/WEB-INF/views/profile/edit.jsp").forward(req, resp);
                return;
            }
            // Check email uniqueness
            User existingByEmail = userDAO.findByEmail(email);
            if (existingByEmail != null && existingByEmail.getId() != userId) {
                req.setAttribute("error", "Email already in use.");
                req.setAttribute("user", user);
                req.getRequestDispatcher("/WEB-INF/views/profile/edit.jsp").forward(req, resp);
                return;
            }

            String passwordHash = user.getPasswordHash();
            if (newPassword != null && !newPassword.isEmpty()) {
                if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) {
                    req.setAttribute("error", "Current password is incorrect.");
                    req.setAttribute("user", user);
                    req.getRequestDispatcher("/WEB-INF/views/profile/edit.jsp").forward(req, resp);
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    req.setAttribute("error", "New passwords do not match.");
                    req.setAttribute("user", user);
                    req.getRequestDispatcher("/WEB-INF/views/profile/edit.jsp").forward(req, resp);
                    return;
                }
                passwordHash = PasswordUtil.hash(newPassword);
            }

            userDAO.updateUser(userId, username.trim(), email.trim(), passwordHash);
            req.getSession().setAttribute("username", username.trim());
            req.getSession().setAttribute("successMessage", "Profile updated successfully.");
            resp.sendRedirect(req.getContextPath() + "/profile");
        } catch (Exception e) {
            req.setAttribute("error", "Error updating profile: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
