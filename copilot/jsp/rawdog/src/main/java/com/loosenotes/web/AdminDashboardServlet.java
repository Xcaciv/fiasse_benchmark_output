package com.loosenotes.web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/admin")
public class AdminDashboardServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        String userQuery = request.getParameter("userQuery") == null ? "" : request.getParameter("userQuery").trim();
        request.setAttribute("pageTitle", "Admin Dashboard");
        request.setAttribute("userQuery", userQuery);
        request.setAttribute("userCount", app().getUserDao().countUsers());
        request.setAttribute("noteCount", app().getNoteDao().countNotes());
        request.setAttribute("users", app().getUserDao().searchUsers(userQuery));
        request.setAttribute("allUsers", app().getUserDao().listAll());
        request.setAttribute("recentNotes", app().getNoteDao().listRecentNotesForAdmin(20));
        request.setAttribute("recentActivity", app().getActivityLogDao().recent(20));
        render(request, response, "admin/dashboard");
    }
}
