package com.loosenotes.web;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.security.SecurityUtil;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "SharedNoteServlet", urlPatterns = {"/shared/view"})
public class SharedNoteServlet extends BaseServlet {
    private final NoteDao noteDao = new NoteDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = com.loosenotes.util.AppUtil.trimToEmpty(request.getParameter("token"));
        if (token.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing share token.");
            return;
        }
        try {
            Note note = noteDao.findNoteByShareTokenHash(SecurityUtil.sha256Base64(token)).orElse(null);
            if (note == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Share link is invalid or has been revoked.");
                return;
            }
            User user = currentUser(request);
            request.setAttribute("note", note);
            request.setAttribute("attachments", noteDao.listAttachments(note.getId()));
            request.setAttribute("ratings", noteDao.listRatings(note.getId()));
            request.setAttribute("myRating", user == null ? null : noteDao.findUserRating(note.getId(), user.getId()).orElse(null));
            request.setAttribute("shareMode", true);
            request.setAttribute("shareToken", token);
            request.setAttribute("activeShareLink", null);
            render(request, response, "notes/detail.jsp", note.getTitle());
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
