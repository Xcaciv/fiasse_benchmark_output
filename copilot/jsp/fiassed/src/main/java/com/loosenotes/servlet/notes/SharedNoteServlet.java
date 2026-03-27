package com.loosenotes.servlet.notes;

import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.service.AttachmentService;
import com.loosenotes.service.ShareLinkService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@WebServlet("/share/*")
public class SharedNoteServlet extends HttpServlet {
    private final ShareLinkService shareLinkService = new ShareLinkService();
    private final AttachmentService attachmentService = new AttachmentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String token = pathInfo.substring(1);
        if (token.length() > 100) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ip = getClientIp(req);
        Optional<Note> noteOpt = shareLinkService.findNoteByToken(token, ip);
        if (noteOpt.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Note note = noteOpt.get();
        List<Attachment> attachments = attachmentService.findByNoteId(note.getId());
        req.setAttribute("note", note);
        req.setAttribute("attachments", attachments);
        resp.setHeader("Cache-Control", "no-store, no-cache");
        req.getRequestDispatcher("/WEB-INF/jsp/share/shared-note.jsp").forward(req, resp);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
