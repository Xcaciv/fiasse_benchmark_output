package com.loosenotes.servlet;

import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Handles full-text note search.
 * URL: GET /search?q={query}
 *
 * SSEM notes:
 * - Confidentiality: search only returns owned + public notes (enforced by NoteDao).
 * - Integrity: query sanitized before service call.
 */
public class SearchServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getRequiredUser(req, resp);
        if (user == null) return;

        String rawQuery = req.getParameter("q");
        String query = InputSanitizer.sanitizeLine(rawQuery);

        List<Note> results = Collections.emptyList();
        if (query != null && !query.isBlank()) {
            try {
                results = getNoteService().search(query, user.getId());
            } catch (ServiceException e) {
                req.setAttribute("error", e.getMessage());
            } catch (SQLException e) {
                log.error("Error during note search", e);
                req.setAttribute("error", "Search is temporarily unavailable");
            }
        }

        req.setAttribute("query", query != null ? query : "");
        req.setAttribute("results", results);
        forward(req, resp, "notes/search.jsp");
    }
}
