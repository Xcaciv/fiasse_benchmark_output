package com.loosenotes.servlet;

import com.loosenotes.dao.DatabaseManager;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles user authentication.
 * Failed attempts are logged with IP; session is created only on successful authentication.
 * FIASSE Authenticity: new session ID on login prevents session fixation.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String ATTEMPT_ATTR = "loginAttempts";
    private static final int MAX_ATTEMPTS = 10;

    private UserDao userDao;

    @Override
    public void init() {
        this.userDao = new UserDao(DatabaseManager.getInstance());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        if (req.getSession(false) != null &&
                req.getSession(false).getAttribute("currentUser") != null) {
            res.sendRedirect(req.getContextPath() + "/notes");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        // Track attempts per session to slow brute-force
        HttpSession tempSession = req.getSession(true);
        int attempts = getAttemptCount(tempSession);

        if (attempts >= MAX_ATTEMPTS) {
            req.setAttribute("error", "Too many failed attempts. Please try again later.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
            return;
        }

        String username = ValidationUtil.sanitizeString(req.getParameter("username"));
        String password = req.getParameter("password");

        if (ValidationUtil.isBlank(username) || ValidationUtil.isBlank(password)) {
            recordFailedAttempt(tempSession, username, req.getRemoteAddr());
            req.setAttribute("error", "Invalid username or password.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
            return;
        }

        try {
            authenticateUser(req, res, username, password, tempSession);
        } catch (SQLException e) {
            getServletContext().log("Login error", e);
            req.setAttribute("error", "Login failed. Please try again.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
        }
    }

    private void authenticateUser(HttpServletRequest req, HttpServletResponse res,
                                   String username, String password, HttpSession tempSession)
            throws SQLException, IOException, ServletException {
        User user = userDao.findByUsername(username).orElse(null);

        if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            recordFailedAttempt(tempSession, username, req.getRemoteAddr());
            req.setAttribute("error", "Invalid username or password.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
            return;
        }

        // Session fixation prevention: invalidate old session, create new one
        tempSession.invalidate();
        HttpSession newSession = req.getSession(true);
        newSession.setAttribute("currentUser", user);
        CsrfUtil.generateToken(newSession);
        AuditLogger.logAuthEvent("LOGIN_SUCCESS", username, req.getRemoteAddr(), "");

        String next = req.getParameter("next");
        if (next != null && next.startsWith("/") && !next.startsWith("//")) {
            res.sendRedirect(req.getContextPath() + next);
        } else {
            res.sendRedirect(req.getContextPath() + "/notes");
        }
    }

    private int getAttemptCount(HttpSession session) {
        Object count = session.getAttribute(ATTEMPT_ATTR);
        return (count instanceof Integer) ? (Integer) count : 0;
    }

    private void recordFailedAttempt(HttpSession session, String username, String ip) {
        int count = getAttemptCount(session) + 1;
        session.setAttribute(ATTEMPT_ATTR, count);
        AuditLogger.logAuthEvent("LOGIN_FAILURE", username, ip, "attempts=" + count);
    }
}
