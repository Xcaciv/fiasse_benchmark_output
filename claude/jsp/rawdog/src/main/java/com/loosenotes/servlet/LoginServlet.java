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
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // If already logged in, redirect to notes
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            resp.sendRedirect(req.getContextPath() + "/notes");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String returnUrl = req.getParameter("returnUrl");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            req.setAttribute("error", "Username and password are required.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
            return;
        }

        User user = userDAO.findByUsername(username.trim());
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            req.setAttribute("error", "Invalid username or password.");
            req.setAttribute("username", username);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
            return;
        }

        // Create session
        HttpSession session = req.getSession(true);
        session.setAttribute("currentUser", user);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());
        session.setMaxInactiveInterval(3600); // 1 hour

        auditLogDAO.log(user.getId(), "LOGIN", "User logged in: " + user.getUsername());

        if (returnUrl != null && !returnUrl.isBlank() && returnUrl.startsWith("/")) {
            resp.sendRedirect(req.getContextPath() + returnUrl);
        } else {
            resp.sendRedirect(req.getContextPath() + "/notes");
        }
    }
}
