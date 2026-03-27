package com.loosenotes.servlet.admin;

import com.loosenotes.service.AdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {
    private final AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String role = (String) req.getSession().getAttribute("role");
        if (!"ADMIN".equals(role)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        resp.setHeader("Cache-Control", "no-store, no-cache");
        req.setAttribute("totalUsers", adminService.countUsers());
        req.setAttribute("totalNotes", adminService.countNotes());
        req.setAttribute("recentEvents", adminService.getRecentAuditEvents(50));
        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }
}
