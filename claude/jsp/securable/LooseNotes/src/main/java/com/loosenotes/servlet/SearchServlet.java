package com.loosenotes.servlet;

import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Handles note search functionality.
 * SSEM: Integrity - search queries sanitized in NoteService.
 * SSEM: Confidentiality - private notes from other users never returned.
 */
@WebServlet("/notes/search")
public class SearchServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(SearchServlet.class);
    private static final String SEARCH_JSP = "/WEB-INF/jsp/notes/search.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String query = req.getParameter("q");
        if (query == null || query.isBlank()) {
            forward(req, res, SEARCH_JSP);
            return;
        }

        User user = getCurrentUser(req);
        try {
            List<Note> results = getNoteService().search(query, user.getId());
            req.setAttribute("results", results);
            req.setAttribute("query", query);
            forward(req, res, SEARCH_JSP);
        } catch (SQLException e) {
            log.error("Error during note search", e);
            forwardWithError(req, res, SEARCH_JSP, "Search failed. Please try again.");
        }
    }
}
