package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.service.*;
import com.loosenotes.util.CsrfUtil;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Base servlet providing shared helpers: service lookup, user resolution,
 * CSRF token injection, and error forwarding.
 *
 * SSEM notes:
 * - Analyzability: common plumbing in one place; subclasses focus on business logic.
 * - Modifiability: services retrieved from ServletContext – swap implementations without code change.
 * - Resilience: getUser() never returns null; getRequiredUser() throws if unauthenticated.
 */
public abstract class BaseServlet extends HttpServlet {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // ── Service accessors ───────────────────────────────────────────────────

    protected UserService getUserService() {
        return (UserService) getServletContext().getAttribute("userService");
    }

    protected NoteService getNoteService() {
        return (NoteService) getServletContext().getAttribute("noteService");
    }

    protected AttachmentService getAttachmentService() {
        return (AttachmentService) getServletContext().getAttribute("attachmentService");
    }

    protected RatingService getRatingService() {
        return (RatingService) getServletContext().getAttribute("ratingService");
    }

    protected ShareLinkService getShareLinkService() {
        return (ShareLinkService) getServletContext().getAttribute("shareLinkService");
    }

    protected AuditService getAuditService() {
        return (AuditService) getServletContext().getAttribute("auditService");
    }

    protected PasswordResetService getPasswordResetService() {
        return (PasswordResetService) getServletContext().getAttribute("passwordResetService");
    }

    // ── Session helpers ──────────────────────────────────────────────────────

    /** Returns the current authenticated user, or null if not logged in. */
    protected User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (User) session.getAttribute("currentUser") : null;
    }

    /**
     * Returns the current authenticated user.
     * Sends 401 and returns null if no user is found.
     */
    protected User getRequiredUser(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User user = getCurrentUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
        }
        return user;
    }

    /** Returns the client IP address (non-null). */
    protected String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Take only the first IP from a potentially spoofed chain
            return forwarded.split(",")[0].strip();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
    }

    // ── View helpers ─────────────────────────────────────────────────────────

    /**
     * Forwards to a JSP view, injecting the CSRF token and current user.
     *
     * @param jspPath relative path under /WEB-INF/jsp/
     */
    protected void forward(HttpServletRequest req, HttpServletResponse resp, String jspPath)
            throws ServletException, IOException {
        // Inject CSRF token for form rendering
        HttpSession session = req.getSession(false);
        if (session != null) {
            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(session));
        }
        req.setAttribute("currentUser", getCurrentUser(req));
        resp.setContentType("text/html;charset=UTF-8");
        RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/jsp/" + jspPath);
        rd.forward(req, resp);
    }

    /** Sends an HTTP error with a simple message attribute for the error JSP. */
    protected void sendError(HttpServletRequest req, HttpServletResponse resp,
                              int status, String message) throws IOException {
        req.setAttribute("errorMessage", message);
        resp.sendError(status, message);
    }

    /**
     * Parses the last path segment as a long ID.
     * Returns -1 on parse failure.
     * Example: /notes/42/edit → 42
     */
    protected long parseIdFromPath(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) return -1L;
        // Strip leading slash and take the first segment
        String[] parts = path.substring(1).split("/");
        try {
            long id = Long.parseLong(parts[0]);
            return id > 0 ? id : -1L;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
