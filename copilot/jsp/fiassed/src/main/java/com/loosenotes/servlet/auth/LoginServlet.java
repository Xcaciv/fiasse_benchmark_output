package com.loosenotes.servlet.auth;

import com.loosenotes.model.User;
import com.loosenotes.service.UserService;
import com.loosenotes.util.CsrfUtils;
import com.loosenotes.util.SecurityUtils;
import com.loosenotes.dao.SessionDao;
import com.loosenotes.util.DatabaseManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.util.Optional;

@WebServlet("/auth/login")
public class LoginServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);
    private final UserService userService = new UserService();
    private final SessionDao sessionDao = new SessionDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            resp.sendRedirect(req.getContextPath() + "/notes/list");
            return;
        }
        req.getSession(true);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String ip = getClientIp(req);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            req.setAttribute("error", "Invalid username or password");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
            return;
        }

        // Prevent session fixation: get old session id before auth
        HttpSession oldSession = req.getSession(false);
        String oldSessionId = oldSession != null ? oldSession.getId() : null;

        Optional<User> userOpt = userService.authenticate(username.trim(), password, ip,
                oldSessionId != null ? oldSessionId : "none");

        if (userOpt.isEmpty()) {
            req.setAttribute("error", "Invalid username or password");
            req.getSession(true); // Create new session even on failure
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
            return;
        }

        User user = userOpt.get();

        // Session fixation prevention: invalidate old session, create new
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = req.getSession(true);
        newSession.setAttribute("userId", user.getId());
        newSession.setAttribute("username", user.getUsername());
        newSession.setAttribute("role", user.getRole());
        newSession.setMaxInactiveInterval(30 * 60); // 30 min

        // Generate CSRF token for new session
        CsrfUtils.getOrCreateToken(newSession);

        // Record session in DB
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            sessionDao.insert(conn, user.getId(), SecurityUtils.sha256Hex(newSession.getId()));
        } catch (Exception e) {
            logger.error("Error recording session", e);
        }

        resp.sendRedirect(req.getContextPath() + "/notes/list");
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
