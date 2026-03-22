package com.loosenotes.web;

import com.loosenotes.dao.ActivityLogDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.DashboardStats;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.util.AppUtil;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "AdminServlet", urlPatterns = {"/admin", "/admin/reassign"})
public class AdminServlet extends BaseServlet {
    private final UserDao userDao = new UserDao();
    private final NoteDao noteDao = new NoteDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User admin = requireAdmin(request, response);
            if (admin == null) {
                return;
            }
            request.setAttribute("userQuery", AppUtil.trimToEmpty(request.getParameter("userQuery")));
            request.setAttribute("noteQuery", AppUtil.trimToEmpty(request.getParameter("noteQuery")));
            request.setAttribute("stats", new DashboardStats(userDao.countUsers(), noteDao.countNotes()));
            request.setAttribute("users", userDao.listUsers((String) request.getAttribute("userQuery")));
            request.setAttribute("notes", noteDao.listAdminNotes((String) request.getAttribute("noteQuery")));
            request.setAttribute("assignableUsers", userDao.listAssignableUsers());
            request.setAttribute("activityLogs", activityLogDao.listRecent(20));
            render(request, response, "admin/dashboard.jsp", "Admin dashboard");
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User admin = requireAdmin(request, response);
            if (admin == null || !requireCsrf(request, response)) {
                return;
            }
            if (!"/admin/reassign".equals(request.getServletPath())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            long noteId = Long.parseLong(request.getParameter("noteId"));
            long newOwnerId = Long.parseLong(request.getParameter("newOwnerId"));
            Note note = noteDao.findById(noteId).orElse(null);
            if (note == null || userDao.findById(newOwnerId).isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            noteDao.reassignNote(noteId, newOwnerId);
            activityLogDao.log(admin.getId(), "admin.reassign_note", "Reassigned note #" + noteId + " to user #" + newOwnerId + ".");
            setFlash(request, "success", "Note ownership reassigned.");
            response.sendRedirect(request.getContextPath() + "/admin");
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid reassignment payload.");
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
