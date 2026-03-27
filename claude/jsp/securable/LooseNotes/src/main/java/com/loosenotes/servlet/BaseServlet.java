package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.service.*;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.RateLimiter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Properties;

/**
 * Base servlet providing shared utilities for all application servlets.
 * SSEM: Modifiability - centralized helper methods reduce duplication.
 * SSEM: Analyzability - clear patterns for common operations.
 */
public abstract class BaseServlet extends HttpServlet {

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

    protected RateLimiter getLoginRateLimiter() {
        return (RateLimiter) getServletContext().getAttribute("loginRateLimiter");
    }

    protected Properties getAppConfig() {
        return (Properties) getServletContext().getAttribute("appConfig");
    }

    /** Returns the authenticated user from the session. */
    protected User getCurrentUser(HttpServletRequest req) {
        return (User) req.getAttribute("currentUser");
    }

    /** Returns the client IP, respecting reverse proxy headers. */
    protected String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    /** Forwards to a JSP view. */
    protected void forward(HttpServletRequest req, HttpServletResponse res,
                           String jspPath) throws ServletException, IOException {
        // Inject CSRF token into all forwarded requests
        HttpSession session = req.getSession(false);
        if (session != null) {
            req.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
        }
        req.getRequestDispatcher(jspPath).forward(req, res);
    }

    /** Redirects to a context-relative path. */
    protected void redirect(HttpServletResponse res, HttpServletRequest req,
                             String path) throws IOException {
        res.sendRedirect(req.getContextPath() + path);
    }

    /** Sets an error message in the request and forwards to the given JSP. */
    protected void forwardWithError(HttpServletRequest req, HttpServletResponse res,
                                     String jspPath, String error)
            throws ServletException, IOException {
        req.setAttribute("error", error);
        forward(req, res, jspPath);
    }

    /** Sends a 403 Forbidden response. */
    protected void sendForbidden(HttpServletResponse res) throws IOException {
        res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
    }

    /** Sends a 404 Not Found response. */
    protected void sendNotFound(HttpServletResponse res) throws IOException {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
    }

    /** Returns true and sends 403 if user is not an admin. */
    protected boolean requireAdmin(User user, HttpServletResponse res) throws IOException {
        if (user == null || !user.isAdmin()) {
            sendForbidden(res);
            return true;
        }
        return false;
    }
}
