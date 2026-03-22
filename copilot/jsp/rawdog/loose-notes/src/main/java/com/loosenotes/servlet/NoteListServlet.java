package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/notes")
public class NoteListServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer userId = (Integer) req.getSession(false) != null ?
                (Integer) req.getSession().getAttribute("userId") : null;
        if (userId == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        try {
            List<Note> notes = noteDAO.getNotesByUser(userId);
            req.setAttribute("notes", notes);
            req.getRequestDispatcher("/WEB-INF/views/notes/list.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error loading notes: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
