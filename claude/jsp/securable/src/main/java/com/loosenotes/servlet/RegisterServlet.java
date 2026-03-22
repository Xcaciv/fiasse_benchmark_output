package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
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
 * REQ-001 – User registration.
 * SSEM: Input Validation, Cryptography (BCrypt), Logging (no passwords logged).
 */
public class RegisterServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        // Seed CSRF token before rendering the form
        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
        req.getRequestDispatcher("/jsp/register.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String username = ValidationUtil.truncate(req.getParameter("username"), 50);
        String email    = ValidationUtil.truncate(req.getParameter("email"), 100);
        String password = req.getParameter("password");
        String confirm  = req.getParameter("confirmPassword");

        // ── Input Validation ──────────────────────────────────────────────
        String error = null;
        if (!ValidationUtil.isValidUsername(username)) {
            error = "Username must be 3–50 alphanumeric/underscore characters.";
        } else if (!ValidationUtil.isValidEmail(email)) {
            error = "Invalid email address.";
        } else if (password == null || !password.equals(confirm)) {
            error = "Passwords do not match.";
        } else {
            error = PasswordUtil.validateStrength(password);
        }

        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
            req.getRequestDispatcher("/jsp/register.jsp").forward(req, res);
            return;
        }

        try {
            // Duplicate username / email check
            if (userDAO.findByUsername(username) != null) {
                req.setAttribute("error", "Username already taken.");
                req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
                req.getRequestDispatcher("/jsp/register.jsp").forward(req, res);
                return;
            }
            if (userDAO.findByEmail(email) != null) {
                req.setAttribute("error", "Email already registered.");
                req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
                req.getRequestDispatcher("/jsp/register.jsp").forward(req, res);
                return;
            }

            String hash = PasswordUtil.hash(password);
            userDAO.create(username, email, hash);

            LOGGER.info("New user registered: " + username);
            res.sendRedirect(req.getContextPath() + "/login?registered=1");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Registration error for user: " + username, e);
            req.setAttribute("error", "Registration failed. Please try again.");
            req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
            req.getRequestDispatcher("/jsp/register.jsp").forward(req, res);
        }
    }
}
