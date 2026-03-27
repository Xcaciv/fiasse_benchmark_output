package com.loosenotes.servlet;

import com.loosenotes.dao.DBConnection;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;
import com.loosenotes.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class IndexServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    public void init() throws ServletException {
        try {
            DBConnection.initializeDatabase();
        } catch (Exception e) {
            throw new ServletException("Failed to initialize database", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        List<Note> recentNotes = noteDAO.findRecent(10);
        
        request.setAttribute("recentNotes", recentNotes);
        
        if (SessionUtil.isLoggedIn(session)) {
            request.getRequestDispatcher("/WEB-INF/views/index.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/landing.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
