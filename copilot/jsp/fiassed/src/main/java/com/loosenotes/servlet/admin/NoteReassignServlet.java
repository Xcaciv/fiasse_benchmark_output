package com.loosenotes.servlet.admin;

import com.loosenotes.service.AdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/reassign-note")
public class NoteReassignServlet extends HttpServlet {
    private final AdminService adminService = new AdminService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String role = (String) req.getSession().getAttribute("role");
        if (!"ADMIN".equals(role)) { resp.sendError(403); return; }

        Long adminId = (Long) req.getSession().getAttribute("userId");
        String noteIdParam = req.getParameter("noteId");
        String newOwnerIdParam = req.getParameter("newOwnerId");
        String ip = getClientIp(req);
        String sessionId = req.getSession().getId();

        if (noteIdParam == null || newOwnerIdParam == null) { resp.sendError(400); return; }
        try {
            long noteId = Long.parseLong(noteIdParam);
            long newOwnerId = Long.parseLong(newOwnerIdParam);
            adminService.reassignNote(noteId, newOwnerId, adminId, ip, sessionId);
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        } catch (NumberFormatException e) {
            resp.sendError(400);
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
