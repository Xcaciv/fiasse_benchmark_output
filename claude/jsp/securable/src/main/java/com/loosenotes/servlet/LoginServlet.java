package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-002 – User login.
 * SSEM: Authentication, Session Management, Logging.
 */
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
        req.getRequestDispatcher("/jsp/login.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String username = ValidationUtil.truncate(req.getParameter("username"), 50);
        String password = req.getParameter("password");

        if (ValidationUtil.isBlank(username) || ValidationUtil.isBlank(password)) {
            req.setAttribute("error", "Username and password are required.");
            req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
            req.getRequestDispatcher("/jsp/login.jsp").forward(req, res);
            return;
        }

        try {
            User user = userDAO.findByUsername(username);

            // Always run BCrypt verification to prevent timing-based user enumeration
            boolean valid = user != null && PasswordUtil.verify(password, user.getPasswordHash());

            if (!valid) {
                LOGGER.warning("Failed login attempt for username: " + username
                        + " from IP: " + req.getRemoteAddr());
                req.setAttribute("error", "Invalid username or password.");
                req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
                req.getRequestDispatcher("/jsp/login.jsp").forward(req, res);
                return;
            }

            // ── Session fixation prevention ───────────────────────────────
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) oldSession.invalidate();
            HttpSession session = req.getSession(true);

            session.setAttribute("userId",   user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role",     user.getRole());

            // Regenerate CSRF token for new session
            CsrfUtil.getOrCreateToken(req);

            LOGGER.info("User logged in: " + username + " from IP: " + req.getRemoteAddr());
            res.sendRedirect(req.getContextPath() + "/home");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Login DB error", e);
            req.setAttribute("error", "Login failed. Please try again.");
            req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
            req.getRequestDispatcher("/jsp/login.jsp").forward(req, res);
        }
    }
}
