package com.loosenotes.servlet;

import com.loosenotes.dao.RatingDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/ratings")
public class RatingServlet extends HttpServlet {

    private final RatingDAO ratingDAO = new RatingDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = (Integer) req.getSession().getAttribute("userId");
        try {
            int noteId = Integer.parseInt(req.getParameter("noteId"));
            int rating = Integer.parseInt(req.getParameter("rating"));
            String comment = req.getParameter("comment");

            if (rating < 1 || rating > 5) {
                resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);
                return;
            }
            ratingDAO.addOrUpdateRating(noteId, userId, rating, comment);
            resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);
        } catch (Exception e) {
            req.setAttribute("error", "Error saving rating: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
