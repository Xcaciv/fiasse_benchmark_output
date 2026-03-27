package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.Note;
import com.loosenotes.service.AuditService;
import com.loosenotes.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Serves the top-rated public notes view at {@code GET /top-rated}.
 *
 * <p>Only PUBLIC notes with at least {@value #MIN_RATING_COUNT} ratings are
 * considered. No authentication is required.
 */
@WebServlet("/top-rated")
public class TopRatedServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(TopRatedServlet.class);
    private static final long serialVersionUID = 1L;

    /** Minimum number of ratings a note must have to appear on this page. */
    private static final int MIN_RATING_COUNT = 3;

    /** Maximum number of results to return. */
    private static final int PAGE_SIZE = 20;

    private NoteService noteService;

    @Override
    public void init() throws ServletException {
        noteService = new NoteService(
                new NoteDao(),
                new AuditService(new AuditLogDao())
        );
    }

    // =========================================================================
    // GET /top-rated
    // =========================================================================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            List<Note> notes = noteService.getTopRated(MIN_RATING_COUNT, PAGE_SIZE);
            request.setAttribute("notes", notes);
            request.getRequestDispatcher("/WEB-INF/jsp/note/top-rated.jsp").forward(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
