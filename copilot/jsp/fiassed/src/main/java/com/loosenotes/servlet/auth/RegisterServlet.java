package com.loosenotes.servlet.auth;

import com.loosenotes.service.UserService;
import com.loosenotes.util.CsrfUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/auth/register")
public class RegisterServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession(true);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String ip = getClientIp(req);

        if (username == null || email == null || password == null || confirmPassword == null) {
            req.setAttribute("error", "All fields are required");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        if (password.length() < 12 || password.length() > 64) {
            req.setAttribute("error", "Password must be between 12 and 64 characters");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        boolean success = userService.register(username.trim(), email.trim(), password, ip);
        if (success) {
            HttpSession session = req.getSession(true);
            CsrfUtils.getOrCreateToken(session);
            req.setAttribute("success", "Registration successful. Please log in.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
        } else {
            req.setAttribute("error", "Registration failed. Username or email may already be in use.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
