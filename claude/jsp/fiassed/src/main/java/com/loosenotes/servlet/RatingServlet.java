package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.service.AuditService;
import com.loosenotes.service.RatingService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.CsrfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles note rating operations (submit, edit, delete) at {@code /ratings/*}.
 *
 * <p>All operations require an authenticated session and a valid CSRF token.
 * Ownership is enforced by {@link RatingService}.
 *
 * <p>Routes (POST only):
 * <ul>
 *   <li>{@code POST /ratings/submit/{noteId}}   — submit a new rating</li>
 *   <li>{@code POST /ratings/{ratingId}/edit}   — update an existing rating</li>
 *   <li>{@code POST /ratings/{ratingId}/delete} — delete a rating</li>
 * </ul>
 */
@WebServlet("/ratings/*")
public class RatingServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(RatingServlet.class);
    private static final long serialVersionUID = 1L;

    private RatingService ratingService;

    @Override
    public void init() throws ServletException {
        AuditService auditService = new AuditService(new AuditLogDao());
        ratingService = new RatingService(
                new RatingDao(),
                new NoteDao(),
                auditService
        );
    }

    // =========================================================================
    // POST dispatch
    // =========================================================================

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            Long userId = getRequiredUserId(request, response);
            if (userId == null) return;

            HttpSession session = request.getSession(false);
            if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
                log.warn("CSRF mismatch on rating action. ip={}", request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Strip leading "/" and split.
            String[] parts = pathInfo.split("/");
            // parts[0] == "" (leading "/"), parts[1] == first segment

            if (parts.length == 3 && "submit".equals(parts[1])) {
                // POST /ratings/submit/{noteId}
                Long noteId = parseId(parts[2]);
                if (noteId == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                handleSubmitRating(request, response, session, noteId, userId);

            } else if (parts.length == 3 && "edit".equals(parts[2])) {
                // POST /ratings/{ratingId}/edit
                Long ratingId = parseId(parts[1]);
                if (ratingId == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                handleEditRating(request, response, session, ratingId, userId);

            } else if (parts.length == 3 && "delete".equals(parts[2])) {
                // POST /ratings/{ratingId}/delete
                Long ratingId = parseId(parts[1]);
                if (ratingId == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                handleDeleteRating(request, response, session, ratingId, userId);

            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // Route handlers
    // =========================================================================

    private void handleSubmitRating(HttpServletRequest request, HttpServletResponse response,
                                    HttpSession session, Long noteId, Long userId)
            throws IOException {

        int ratingValue = parseRatingValue(request.getParameter("ratingValue"));
        String comment  = request.getParameter("comment");

        try {
            ratingService.submitRating(noteId, userId, ratingValue, comment);
            CsrfUtil.rotateToken(session);
            response.sendRedirect(request.getContextPath() + "/notes/" + noteId);

        } catch (ServiceException e) {
            log.info("Rating submit failed. noteId={} userId={} code={}", noteId, userId, e.getCode());
            if ("ACCESS_DENIED".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else if ("NOT_FOUND".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                // VALIDATION or DUPLICATE — redirect back with error param.
                try {
                    response.sendRedirect(request.getContextPath() + "/notes/" + noteId
                            + "?ratingError=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));
                } catch (Exception ex) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }
    }

    private void handleEditRating(HttpServletRequest request, HttpServletResponse response,
                                  HttpSession session, Long ratingId, Long userId)
            throws IOException {

        int ratingValue = parseRatingValue(request.getParameter("ratingValue"));
        String comment  = request.getParameter("comment");

        try {
            ratingService.updateRating(ratingId, userId, ratingValue, comment);
            CsrfUtil.rotateToken(session);
            // Redirect to referer if available, else fall back to /notes.
            String referer = request.getHeader("Referer");
            response.sendRedirect(referer != null ? referer : request.getContextPath() + "/notes");

        } catch (ServiceException e) {
            log.info("Rating edit failed. ratingId={} userId={} code={}", ratingId, userId, e.getCode());
            if ("ACCESS_DENIED".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else if ("NOT_FOUND".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    private void handleDeleteRating(HttpServletRequest request, HttpServletResponse response,
                                    HttpSession session, Long ratingId, Long userId)
            throws IOException {

        try {
            ratingService.deleteRating(ratingId, userId);
            CsrfUtil.rotateToken(session);
            String referer = request.getHeader("Referer");
            response.sendRedirect(referer != null ? referer : request.getContextPath() + "/notes");

        } catch (ServiceException e) {
            log.info("Rating delete failed. ratingId={} userId={} code={}", ratingId, userId, e.getCode());
            if ("ACCESS_DENIED".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else if ("NOT_FOUND".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Long getRequiredUserId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            if (userId != null) return userId;
        }
        response.sendRedirect(request.getContextPath() + "/auth/login");
        return null;
    }

    private Long parseId(String segment) {
        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses the rating value from a request parameter, defaulting to 0 on error
     * (which will be caught by service-layer validation).
     */
    private int parseRatingValue(String param) {
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException | NullPointerException e) {
            return 0; // Invalid — RatingService will reject this.
        }
    }
}
