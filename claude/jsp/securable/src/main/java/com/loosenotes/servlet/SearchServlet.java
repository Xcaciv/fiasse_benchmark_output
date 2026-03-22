package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-012 – Note search.
 * Returns owned notes (any visibility) + public notes matching the keyword.
 * SSEM: Input Validation (keyword sanitised via PreparedStatement LIKE).
 */
public class SearchServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SearchServlet.class.getName());
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long userId = (long) req.getSession().getAttribute("userId");
        String keyword = ValidationUtil.truncate(req.getParameter("q"), 200);
        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));

        if (ValidationUtil.isBlank(keyword)) {
            req.setAttribute("results", java.util.Collections.emptyList());
            req.getRequestDispatcher("/jsp/search.jsp").forward(req, res);
            return;
        }

        try {
            req.setAttribute("results", noteDAO.search(userId, keyword));
            req.setAttribute("keyword", keyword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Search error", e);
            req.setAttribute("error", "Search failed. Please try again.");
        }

        req.getRequestDispatcher("/jsp/search.jsp").forward(req, res);
    }
}
