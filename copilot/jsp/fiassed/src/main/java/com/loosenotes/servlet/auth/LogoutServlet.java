package com.loosenotes.servlet.auth;

import com.loosenotes.dao.SessionDao;
import com.loosenotes.util.AuditLogger;
import com.loosenotes.util.DatabaseManager;
import com.loosenotes.util.SecurityUtils;
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

@WebServlet("/auth/logout")
public class LogoutServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LogoutServlet.class);
    private final SessionDao sessionDao = new SessionDao();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            String sessionId = session.getId();
            String ip = getClientIp(req);
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                sessionDao.deleteBySessionHash(conn, SecurityUtils.sha256Hex(sessionId));
            } catch (Exception e) {
                logger.error("Error removing session from DB", e);
            }
            auditLogger.log("LOGOUT", userId, null, ip, "SUCCESS", sessionId, null);
            session.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
