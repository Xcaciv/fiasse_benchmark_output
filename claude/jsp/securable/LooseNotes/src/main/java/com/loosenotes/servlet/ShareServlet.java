package com.loosenotes.servlet;

import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.model.User;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles share link generation, revocation, and public note viewing via token.
 * URL patterns:
 *   GET  /share/{token}           – public view of shared note (no auth required)
 *   POST /share/{noteId}/generate – generate a share link for a note
 *   POST /share/{noteId}/revoke   – revoke the share link for a note
 *
 * SSEM notes:
 * - Authenticity: token is 256-bit random; any tampering yields a 404.
 * - Confidentiality: shared view shows note content without revealing user session info.
 */
public class ShareServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        // Public access – no authentication required
        // /share/{token}
        String token = path.length() > 1 ? path.substring(1) : null;
        if (token == null || token.isBlank()) {
            resp.sendError(404);
            return;
        }
        // Sanitize token (strip control chars); validate hex format implicitly via service
        token = InputSanitizer.sanitizeLine(token);
        try {
            Note note = getShareLinkService().getNoteByToken(token);
            req.setAttribute("note", note);
            // Attachments visible on public share view
            User viewer = getCurrentUser(req);
            long viewerId = viewer != null ? viewer.getId() : -1;
            forward(req, resp, "share/view.jsp");
        } catch (ServiceException e) {
            resp.sendError(404, "Share link not found or revoked");
        } catch (SQLException e) {
            log.error("Error loading shared note for token", e);
            resp.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getRequiredUser(req, resp);
        if (user == null) return;

        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();

        if (path.endsWith("/generate")) {
            generateLink(req, resp, user);
        } else if (path.endsWith("/revoke")) {
            revokeLink(req, resp, user);
        } else {
            resp.sendError(404);
        }
    }

    private void generateLink(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        long noteId = parseIdFromPath(req);
        if (noteId < 0) { resp.sendError(404); return; }
        try {
            ShareLink link = getShareLinkService().generateLink(noteId, user.getId(), getClientIp(req));
            String baseUrl = (String) req.getServletContext().getAttribute("appBaseUrl");
            req.getSession().setAttribute("newShareUrl",
                    baseUrl + req.getContextPath() + "/share/" + link.getToken());
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException e) {
            sendError(req, resp, 403, e.getMessage());
        } catch (SQLException e) {
            log.error("Error generating share link for note {}", noteId, e);
            sendError(req, resp, 500, "Could not generate share link");
        }
    }

    private void revokeLink(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        long noteId = parseIdFromPath(req);
        if (noteId < 0) { resp.sendError(404); return; }
        try {
            getShareLinkService().revokeLink(noteId, user.getId(), getClientIp(req));
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException e) {
            sendError(req, resp, 403, e.getMessage());
        } catch (SQLException e) {
            log.error("Error revoking share link for note {}", noteId, e);
            sendError(req, resp, 500, "Could not revoke share link");
        }
    }
}
