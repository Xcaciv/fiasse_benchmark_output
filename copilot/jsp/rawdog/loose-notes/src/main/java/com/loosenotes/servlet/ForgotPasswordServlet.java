package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.TokenUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        try {
            User user = userDAO.findByEmail(email);
            if (user != null) {
                String token = TokenUtil.generateToken();
                String expiresAt = LocalDateTime.now().plusHours(1).toString();
                saveResetToken(user.getId(), token, expiresAt);
                String resetLink = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                        + req.getContextPath() + "/reset-password?token=" + token;
                req.setAttribute("resetLink", resetLink);
                req.setAttribute("successMessage", "Password reset link generated (demo mode - normally this would be emailed).");
            } else {
                req.setAttribute("successMessage", "If that email exists, a reset link has been sent.");
            }
        } catch (Exception e) {
            req.setAttribute("error", "Error processing request: " + e.getMessage());
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(req, resp);
    }

    private void saveResetToken(int userId, String token, String expiresAt) throws Exception {
        String sql = "INSERT INTO password_reset_tokens (user_id, token, expires_at, used) VALUES (?, ?, ?, 0)";
        try (Connection conn = com.loosenotes.dao.DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setString(3, expiresAt);
            ps.executeUpdate();
        }
    }
}
