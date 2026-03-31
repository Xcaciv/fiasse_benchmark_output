package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.InputSanitizer;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles note rating submission.
 * URL pattern:
 *   POST /ratings/{noteId} – submit or update a rating
 *
 * SSEM notes:
 * - Integrity: stars validated via ValidationUtil before reaching service.
 * - Derived Integrity: noteId from path (server-side), not from form body.
 */
public class RatingServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getRequiredUser(req, resp);
        if (user == null) return;

        long noteId = parseIdFromPath(req);
        if (noteId < 0) { resp.sendError(404); return; }

        // Trust boundary: parse and validate stars from request
        String starsParam = InputSanitizer.sanitizeLine(req.getParameter("stars"));
        int    stars      = ValidationUtil.parseLongId(starsParam) > 0
                ? (int) ValidationUtil.parseLongId(starsParam) : 0;
        String comment = InputSanitizer.sanitizeMultiline(req.getParameter("comment"));
        if (comment != null && comment.length() > 1000) {
            comment = comment.substring(0, 1000);
        }

        try {
            getRatingService().rate(noteId, user.getId(), stars, comment, getClientIp(req));
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException e) {
            sendError(req, resp, 400, e.getMessage());
        } catch (SQLException e) {
            log.error("Error saving rating for note {}", noteId, e);
            sendError(req, resp, 500, "Could not save rating");
        }
    }
}
