package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.Note;
import com.loosenotes.service.AuditService;
import com.loosenotes.service.NoteService;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Handles full-text note search at {@code GET /search}.
 *
 * <p>The query string is validated before being passed to the service layer.
 * Authenticated users see their own private notes in results; unauthenticated
 * visitors see only PUBLIC notes. This is enforced by {@code NoteService},
 * which passes a nullable {@code requestingUserId} to the DAO.
 */
@WebServlet("/search")
public class SearchServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SearchServlet.class);
    private static final long serialVersionUID = 1L;

    private static final int PAGE_SIZE = 10;

    private NoteService noteService;

    @Override
    public void init() throws ServletException {
        noteService = new NoteService(
                new NoteDao(),
                new AuditService(new AuditLogDao())
        );
    }

    // =========================================================================
    // GET /search
    // =========================================================================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            String q = request.getParameter("q");
            int page = parsePage(request);

            // Validate search query before any service call.
            if (q == null || !ValidationUtil.isValidSearchQuery(q)) {
                request.setAttribute("error",
                        "Search query must be between 2 and 500 characters.");
                request.setAttribute("query", q);
                request.setAttribute("page", page);
                request.getRequestDispatcher("/WEB-INF/jsp/note/search.jsp").forward(request, response);
                return;
            }

            // userId may be null for unauthenticated visitors.
            HttpSession session = request.getSession(false);
            Long userId = session != null ? (Long) session.getAttribute("userId") : null;

            List<Note> notes = noteService.searchNotes(userId, q.trim(), page, PAGE_SIZE);

            request.setAttribute("notes", notes);
            request.setAttribute("query", q);
            request.setAttribute("page", page);
            request.getRequestDispatcher("/WEB-INF/jsp/note/search.jsp").forward(request, response);

        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private int parsePage(HttpServletRequest request) {
        try {
            int p = Integer.parseInt(request.getParameter("page"));
            return p > 0 ? p : 1;
        } catch (NumberFormatException | NullPointerException e) {
            return 1;
        }
    }
}
