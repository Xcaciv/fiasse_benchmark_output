package com.loosenotes.servlet.notes;

import com.loosenotes.service.NoteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/notes/create")
public class NoteCreateServlet extends HttpServlet {
    private final NoteService noteService = new NoteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/notes/create.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Owner always from session, never from request
        Long userId = (Long) req.getSession().getAttribute("userId");
        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String visibility = req.getParameter("visibility");
        String ip = getClientIp(req);
        String sessionId = req.getSession().getId();

        // Validate title
        if (title == null || title.isBlank() || title.length() > 200) {
            req.setAttribute("error", "Title is required and must be at most 200 characters");
            req.getRequestDispatcher("/WEB-INF/jsp/notes/create.jsp").forward(req, resp);
            return;
        }
        // Validate content
        if (content == null || content.isBlank() || content.length() > 50000) {
            req.setAttribute("error", "Content is required and must be at most 50,000 characters");
            req.getRequestDispatcher("/WEB-INF/jsp/notes/create.jsp").forward(req, resp);
            return;
        }
        // Validate visibility
        if (!"PUBLIC".equals(visibility) && !"PRIVATE".equals(visibility)) {
            visibility = "PRIVATE";
        }

        long noteId = noteService.createNote(userId, title.trim(), content, visibility, ip, sessionId);
        if (noteId > 0) {
            resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);
        } else {
            req.setAttribute("error", "Failed to create note. Please try again.");
            req.getRequestDispatcher("/WEB-INF/jsp/notes/create.jsp").forward(req, resp);
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
