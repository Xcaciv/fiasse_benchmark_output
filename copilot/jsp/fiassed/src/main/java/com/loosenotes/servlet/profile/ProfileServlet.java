package com.loosenotes.servlet.profile;

import com.loosenotes.model.User;
import com.loosenotes.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long userId = (Long) req.getSession().getAttribute("userId");
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) { resp.sendError(404); return; }
        req.setAttribute("user", userOpt.get());
        req.getRequestDispatcher("/WEB-INF/jsp/profile/profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long userId = (Long) req.getSession().getAttribute("userId");
        String action = req.getParameter("action");
        String ip = getClientIp(req);
        String sessionId = req.getSession().getId();

        if ("changePassword".equals(action)) {
            String oldPassword = req.getParameter("oldPassword");
            String newPassword = req.getParameter("newPassword");
            String confirmPassword = req.getParameter("confirmPassword");

            if (newPassword == null || !newPassword.equals(confirmPassword)) {
                req.setAttribute("error", "Passwords do not match");
            } else if (newPassword.length() < 12 || newPassword.length() > 64) {
                req.setAttribute("error", "Password must be 12-64 characters");
            } else {
                boolean success = userService.changePassword(userId, oldPassword, newPassword, ip, sessionId);
                if (success) {
                    req.getSession().invalidate();
                    resp.sendRedirect(req.getContextPath() + "/auth/login?passwordChanged=true");
                    return;
                } else {
                    req.setAttribute("error", "Current password is incorrect");
                }
            }
        }

        Optional<User> userOpt = userService.findById(userId);
        req.setAttribute("user", userOpt.orElse(null));
        req.getRequestDispatcher("/WEB-INF/jsp/profile/profile.jsp").forward(req, resp);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
