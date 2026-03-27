package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");
        req.setAttribute("user", currentUser);
        req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");

        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String currentPassword = req.getParameter("currentPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");

        if (username == null || username.isBlank() || email == null || email.isBlank()) {
            req.setAttribute("error", "Username and email are required.");
            req.setAttribute("user", currentUser);
            req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
            return;
        }

        User existingByUsername = userDAO.findByUsername(username.trim());
        if (existingByUsername != null && existingByUsername.getId() != currentUser.getId()) {
            req.setAttribute("error", "Username already taken.");
            req.setAttribute("user", currentUser);
            req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
            return;
        }

        User existingByEmail = userDAO.findByEmail(email.trim());
        if (existingByEmail != null && existingByEmail.getId() != currentUser.getId()) {
            req.setAttribute("error", "Email already in use.");
            req.setAttribute("user", currentUser);
            req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
            return;
        }

        currentUser.setUsername(username.trim());
        currentUser.setEmail(email.trim());

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !PasswordUtil.verify(currentPassword, currentUser.getPasswordHash())) {
                req.setAttribute("error", "Current password is incorrect.");
                req.setAttribute("user", currentUser);
                req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                req.setAttribute("error", "New passwords do not match.");
                req.setAttribute("user", currentUser);
                req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
                return;
            }
            if (newPassword.length() < 6) {
                req.setAttribute("error", "Password must be at least 6 characters.");
                req.setAttribute("user", currentUser);
                req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
                return;
            }
            currentUser.setPasswordHash(PasswordUtil.hash(newPassword));
        }

        if (userDAO.update(currentUser)) {
            req.getSession().setAttribute("currentUser", currentUser);
            req.getSession().setAttribute("username", currentUser.getUsername());
            auditLogDAO.log(currentUser.getId(), "PROFILE_UPDATE", "Profile updated");
            req.setAttribute("success", "Profile updated successfully.");
        } else {
            req.setAttribute("error", "Failed to update profile.");
        }

        req.setAttribute("user", currentUser);
        req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
    }
}
