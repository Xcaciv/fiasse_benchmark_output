package com.loosenotes.servlet;

import com.loosenotes.model.Note;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Public share link handler - no authentication required.
 * SSEM: Authenticity - token validated before exposing note.
 * SSEM: Integrity - token format validated to prevent injection.
 * SSEM: Confidentiality - invalid tokens get identical 404 response.
 */
@WebServlet("/share/*")
public class ShareServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(ShareServlet.class);
    private static final String SHARE_VIEW_JSP = "/WEB-INF/jsp/share/view.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            sendNotFound(res);
            return;
        }

        // Extract token from path: /share/{token}
        String rawToken = pathInfo.substring(1);

        // SSEM: Integrity - validate token format before any DB lookup
        if (!ValidationUtil.isValidToken(rawToken)) {
            sendNotFound(res); // Uniform response - no information leakage
            return;
        }

        try {
            Optional<Long> noteId = getShareLinkService().resolveToken(rawToken);
            if (noteId.isEmpty()) {
                sendNotFound(res);
                return;
            }

            Optional<Note> note = getNoteService().getNoteForShare(noteId.get());
            if (note.isEmpty()) {
                sendNotFound(res);
                return;
            }

            req.setAttribute("note", note.get());
            forward(req, res, SHARE_VIEW_JSP);
        } catch (SQLException e) {
            log.error("Error resolving share token", e);
            sendNotFound(res); // SSEM: Confidentiality - don't reveal error details
        }
    }
}
