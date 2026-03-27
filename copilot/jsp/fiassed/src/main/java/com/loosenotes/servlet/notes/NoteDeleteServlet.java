package com.loosenotes.servlet.notes;

import com.loosenotes.service.NoteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/notes/delete")
public class NoteDeleteServlet extends HttpServlet {
    private final NoteService noteService = new NoteService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long userId = (Long) req.getSession().getAttribute("userId");
        String idParam = req.getParameter("id");
        String ip = getClientIp(req);
        String sessionId = req.getSession().getId();

        if (idParam == null) { resp.sendError(404); return; }
        long noteId;
        try { noteId = Long.parseLong(idParam); } catch (NumberFormatException e) { resp.sendError(404); return; }

        boolean success = noteService.deleteNote(noteId, userId, ip, sessionId);
        if (success) {
            resp.sendRedirect(req.getContextPath() + "/notes/list");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
