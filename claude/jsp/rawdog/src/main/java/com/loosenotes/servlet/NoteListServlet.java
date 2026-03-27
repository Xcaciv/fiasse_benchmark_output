package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/notes")
public class NoteListServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");
        List<Note> notes = noteDAO.findByUserId(currentUser.getId());
        req.setAttribute("notes", notes);
        req.getRequestDispatcher("/WEB-INF/jsp/note/list.jsp").forward(req, resp);
    }
}
