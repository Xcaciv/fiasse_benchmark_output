package com.loosenotes.servlet.rating;

import com.loosenotes.service.RatingService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/notes/rate")
public class RatingServlet extends HttpServlet {
    private final RatingService ratingService = new RatingService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long userId = (Long) req.getSession().getAttribute("userId");
        String noteIdParam = req.getParameter("noteId");
        String ratingParam = req.getParameter("rating");
        String comment = req.getParameter("comment");

        if (noteIdParam == null || ratingParam == null) { resp.sendError(400); return; }

        long noteId;
        int rating;
        try {
            noteId = Long.parseLong(noteIdParam);
            rating = Integer.parseInt(ratingParam);
        } catch (NumberFormatException e) {
            resp.sendError(400);
            return;
        }

        if (comment != null && comment.length() > 1000) {
            comment = comment.substring(0, 1000);
        }

        ratingService.rateNote(noteId, userId, rating, comment);
        resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);
    }
}
