package com.loosenotes.servlet;

import com.loosenotes.service.NoteService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.CsrfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Note rating submission (REQ-010, REQ-011).
 * POST /ratings/submit?noteId={id}&stars={1-5}&comment={text}
 */
public final class RatingServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(RatingServlet.class);

    private NoteService noteService;

    @Override
    public void init() {
        this.noteService = (NoteService) getServletContext().getAttribute("noteService");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (!"/submit".equals(pathInfo)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long noteId = parseLong(req.getParameter("noteId"), -1);
        if (noteId < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid noteId");
            return;
        }
        long userId     = (Long) req.getSession().getAttribute("userId");
        String starsStr = req.getParameter("stars");
        String comment  = req.getParameter("comment");

        try {
            noteService.addOrUpdateRating(noteId, userId,
                    Integer.parseInt(starsStr), comment);
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException | NumberFormatException e) {
            log.warn("Rating submission failed: {}", e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private long parseLong(String s, long defaultVal) {
        if (s == null) return defaultVal;
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return defaultVal; }
    }
}
