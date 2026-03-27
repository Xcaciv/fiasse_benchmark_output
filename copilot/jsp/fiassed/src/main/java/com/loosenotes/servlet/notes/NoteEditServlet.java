package com.loosenotes.servlet.notes;

import com.loosenotes.model.Note;
import com.loosenotes.service.NoteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/notes/edit")
public class NoteEditServlet extends HttpServlet {
    private final NoteService noteService = new NoteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long userId = (Long) req.getSession().getAttribute("userId");
        String idParam = req.getParameter("id");
        if (idParam == null) { resp.sendError(404); return; }
        long noteId;
        try { noteId = Long.parseLong(idParam); } catch (NumberFormatException e) { resp.sendError(404); return; }

        Optional<Note> noteOpt = noteService.findById(noteId);
        if (noteOpt.isEmpty() || !noteOpt.get().getUserId().equals(userId)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("note", noteOpt.get());
        req.getRequestDispatcher("/WEB-INF/jsp/notes/edit.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long userId = (Long) req.getSession().getAttribute("userId");
        String idParam = req.getParameter("id");
        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String visibility = req.getParameter("visibility");
        String ip = getClientIp(req);
        String sessionId = req.getSession().getId();

        if (idParam == null) { resp.sendError(404); return; }
        long noteId;
        try { noteId = Long.parseLong(idParam); } catch (NumberFormatException e) { resp.sendError(404); return; }

        if (title == null || title.isBlank() || title.length() > 200) {
            req.setAttribute("error", "Title is required and must be at most 200 characters");
            req.setAttribute("note", noteService.findById(noteId).orElse(null));
            req.getRequestDispatcher("/WEB-INF/jsp/notes/edit.jsp").forward(req, resp);
            return;
        }
        if (content == null || content.isBlank() || content.length() > 50000) {
            req.setAttribute("error", "Content is required and must be at most 50,000 characters");
            req.setAttribute("note", noteService.findById(noteId).orElse(null));
            req.getRequestDispatcher("/WEB-INF/jsp/notes/edit.jsp").forward(req, resp);
            return;
        }
        if (!"PUBLIC".equals(visibility) && !"PRIVATE".equals(visibility)) visibility = "PRIVATE";

        boolean success = noteService.updateNote(noteId, userId, title.trim(), content, visibility, ip, sessionId);
        if (success) {
            resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);
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
