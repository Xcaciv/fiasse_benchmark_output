package com.loosenotes.servlet;

import com.loosenotes.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Top-rated public notes page (REQ-015).
 * GET /top-rated
 */
public final class TopRatedServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(TopRatedServlet.class);

    private NoteService noteService;

    @Override
    public void init() {
        this.noteService = (NoteService) getServletContext().getAttribute("noteService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("topNotes", noteService.findTopRated(20));
        req.getRequestDispatcher("/WEB-INF/jsp/note/top-rated.jsp").forward(req, resp);
    }
}
