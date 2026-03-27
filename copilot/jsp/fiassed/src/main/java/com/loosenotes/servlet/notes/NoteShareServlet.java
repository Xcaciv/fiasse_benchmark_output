package com.loosenotes.servlet.notes;

import com.loosenotes.service.ShareLinkService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/notes/share")
public class NoteShareServlet extends HttpServlet {
    private final ShareLinkService shareLinkService = new ShareLinkService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long userId = (Long) req.getSession().getAttribute("userId");
        String action = req.getParameter("action");
        String idParam = req.getParameter("noteId");
        String ip = getClientIp(req);
        String sessionId = req.getSession().getId();

        if (idParam == null) { resp.sendError(404); return; }
        long noteId;
        try { noteId = Long.parseLong(idParam); } catch (NumberFormatException e) { resp.sendError(404); return; }

        if ("generate".equals(action)) {
            Optional<String> token = shareLinkService.generateShareLink(noteId, userId, ip, sessionId);
            if (token.isPresent()) {
                resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId + "&shareGenerated=true");
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        } else if ("revoke".equals(action)) {
            shareLinkService.revokeShareLink(noteId, userId, ip, sessionId);
            resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId + "&shareRevoked=true");
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
