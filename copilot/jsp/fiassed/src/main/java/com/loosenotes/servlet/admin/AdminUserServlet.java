package com.loosenotes.servlet.admin;

import com.loosenotes.service.AdminService;
import com.loosenotes.util.AuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/users")
public class AdminUserServlet extends HttpServlet {
    private final AdminService adminService = new AdminService();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String role = (String) req.getSession().getAttribute("role");
        if (!"ADMIN".equals(role)) { resp.sendError(403); return; }

        resp.setHeader("Cache-Control", "no-store, no-cache");
        int page = 1;
        try { page = Integer.parseInt(req.getParameter("page")); if (page < 1) page = 1; } catch (Exception e) { page = 1; }
        req.setAttribute("users", adminService.listUsers(page, 20));
        req.setAttribute("totalUsers", adminService.countUsers());
        req.setAttribute("currentPage", page);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/users.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String role = (String) req.getSession().getAttribute("role");
        if (!"ADMIN".equals(role)) { resp.sendError(403); return; }

        Long adminId = (Long) req.getSession().getAttribute("userId");
        String action = req.getParameter("action");
        String targetIdParam = req.getParameter("userId");
        String ip = getClientIp(req);
        String sessionId = req.getSession().getId();

        if ("deleteUser".equals(action) && targetIdParam != null) {
            try {
                long targetId = Long.parseLong(targetIdParam);
                adminService.deleteUser(targetId, adminId, ip, sessionId);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
