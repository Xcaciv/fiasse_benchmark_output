package com.loosenotes.web;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "HomeServlet", urlPatterns = {"/"})
public class HomeServlet extends BaseServlet {
    private final NoteDao noteDao = new NoteDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = currentUser(request);
            request.setAttribute("recentPublicNotes", noteDao.listRecentPublic(8));
            if (user != null) {
                request.setAttribute("myNotesPreview", noteDao.listOwnerNotes(user.getId()).stream().limit(5).collect(Collectors.toList()));
            }
            render(request, response, "index.jsp", "Loose Notes");
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
