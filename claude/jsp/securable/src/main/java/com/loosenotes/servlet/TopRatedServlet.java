package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.util.CsrfUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-015 – Top-rated public notes (min 3 ratings).
 */
public class TopRatedServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TopRatedServlet.class.getName());
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));

        try {
            req.setAttribute("topNotes", noteDAO.findTopRated());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading top-rated notes", e);
            req.setAttribute("topNotes", java.util.Collections.emptyList());
        }
        req.getRequestDispatcher("/jsp/toprated.jsp").forward(req, res);
    }
}
