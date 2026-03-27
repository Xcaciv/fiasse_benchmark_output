package com.loosenotes.servlet;

import com.loosenotes.dao.PasswordResetTokenDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            req.setAttribute("error", "Invalid or missing reset token.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        int userId = tokenDAO.validateToken(token);
        if (userId <= 0) {
            req.setAttribute("error", "Reset token is invalid or has expired.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("token", token);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (token == null || token.isBlank()) {
            req.setAttribute("error", "Invalid token.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        int userId = tokenDAO.validateToken(token);
        if (userId <= 0) {
            req.setAttribute("error", "Reset token is invalid or has expired.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        if (password == null || password.length() < 6) {
            req.setAttribute("error", "Password must be at least 6 characters.");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }
        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match.");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        User user = userDAO.findById(userId);
        if (user == null) {
            req.setAttribute("error", "User not found.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        user.setPasswordHash(PasswordUtil.hash(password));
        if (userDAO.update(user)) {
            tokenDAO.markUsed(token);
            req.getSession(true).setAttribute("flash_success", "Password reset successfully. Please log in.");
            resp.sendRedirect(req.getContextPath() + "/login");
        } else {
            req.setAttribute("error", "Failed to reset password. Please try again.");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
        }
    }
}
