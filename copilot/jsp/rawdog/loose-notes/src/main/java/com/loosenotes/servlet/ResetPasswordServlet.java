package com.loosenotes.servlet;

import com.loosenotes.dao.DatabaseUtil;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        try {
            if (!isValidToken(token)) {
                req.setAttribute("error", "Invalid or expired password reset link.");
                req.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(req, resp);
                return;
            }
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");

        try {
            if (newPassword == null || newPassword.isEmpty() || !newPassword.equals(confirmPassword)) {
                req.setAttribute("error", "Passwords do not match or are empty.");
                req.setAttribute("token", token);
                req.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(req, resp);
                return;
            }
            int userId = getUserIdForToken(token);
            if (userId < 0 || !isValidToken(token)) {
                req.setAttribute("error", "Invalid or expired reset link.");
                req.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(req, resp);
                return;
            }
            var user = userDAO.findById(userId);
            if (user != null) {
                userDAO.updateUser(userId, user.getUsername(), user.getEmail(), PasswordUtil.hash(newPassword));
                markTokenUsed(token);
            }
            HttpSession session = req.getSession(true);
            session.setAttribute("successMessage", "Password reset successfully. Please log in.");
            resp.sendRedirect(req.getContextPath() + "/login");
        } catch (Exception e) {
            req.setAttribute("error", "Error: " + e.getMessage());
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(req, resp);
        }
    }

    private boolean isValidToken(String token) throws Exception {
        if (token == null || token.isEmpty()) return false;
        String sql = "SELECT * FROM password_reset_tokens WHERE token = ? AND used = 0 AND expires_at > ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, LocalDateTime.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int getUserIdForToken(String token) throws Exception {
        String sql = "SELECT user_id FROM password_reset_tokens WHERE token = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("user_id");
            }
        }
        return -1;
    }

    private void markTokenUsed(String token) throws Exception {
        String sql = "UPDATE password_reset_tokens SET used = 1 WHERE token = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }
}
