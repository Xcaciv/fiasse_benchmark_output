package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles rating submissions for notes.
 * SSEM: Integrity - rating value and comment validated.
 */
@WebServlet("/ratings")
public class RatingServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(RatingServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user = getCurrentUser(req);

        long noteId = ValidationUtil.parseLongSafe(req.getParameter("noteId"));
        if (!ValidationUtil.isValidId(noteId)) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid note ID");
            return;
        }

        int value = ValidationUtil.parseIntSafe(req.getParameter("value"), 0);
        String comment = req.getParameter("comment");

        try {
            getRatingService().submitRating(noteId, user.getId(), value, comment);
            redirect(res, req, "/notes/view/" + noteId + "#ratings");
        } catch (IllegalArgumentException e) {
            // Return to note view with error
            redirect(res, req, "/notes/view/" + noteId + "?ratingError=true");
        } catch (SecurityException e) {
            sendForbidden(res);
        } catch (SQLException e) {
            log.error("Error submitting rating for note id={}", noteId, e);
            redirect(res, req, "/notes/view/" + noteId + "?ratingError=true");
        }
    }
}
