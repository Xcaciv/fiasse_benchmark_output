package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.util.CsrfUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Landing page showing the current user's notes. */
public class HomeServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(HomeServlet.class.getName());
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long userId = (long) req.getSession().getAttribute("userId");
        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));

        try {
            req.setAttribute("notes", noteDAO.findByUserId(userId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading home notes", e);
            req.setAttribute("notes", java.util.Collections.emptyList());
        }
        req.getRequestDispatcher("/jsp/home.jsp").forward(req, res);
    }
}
