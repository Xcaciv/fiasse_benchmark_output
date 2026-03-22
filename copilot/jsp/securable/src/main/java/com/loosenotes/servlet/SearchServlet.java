package com.loosenotes.servlet;

import com.loosenotes.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

/**
 * Full-text note search (REQ-012).
 * GET /search?q={keyword}
 */
public final class SearchServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SearchServlet.class);

    private NoteService noteService;

    @Override
    public void init() {
        this.noteService = (NoteService) getServletContext().getAttribute("noteService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String keyword = req.getParameter("q");
        long userId    = (Long) req.getSession().getAttribute("userId");

        if (keyword != null && !keyword.trim().isEmpty()) {
            List<?> results = noteService.search(keyword.trim(), userId);
            req.setAttribute("results", results);
            req.setAttribute("query",   keyword.trim());
        }
        req.getRequestDispatcher("/WEB-INF/jsp/note/search.jsp").forward(req, resp);
    }
}
