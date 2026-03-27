package com.loosenotes.servlet.attachment;

import com.loosenotes.service.AttachmentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.Collection;

@WebServlet("/attachment/upload")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 60 * 1024 * 1024)
public class FileUploadServlet extends HttpServlet {
    private final AttachmentService attachmentService = new AttachmentService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long userId = (Long) req.getSession().getAttribute("userId");
        String noteIdParam = req.getParameter("noteId");
        String ip = getClientIp(req);
        String sessionId = req.getSession().getId();

        if (noteIdParam == null) { resp.sendError(400); return; }
        long noteId;
        try { noteId = Long.parseLong(noteIdParam); } catch (NumberFormatException e) { resp.sendError(400); return; }

        Collection<Part> parts = req.getParts();
        for (Part part : parts) {
            if (part.getName().equals("file") && part.getSize() > 0) {
                String error = attachmentService.uploadAttachment(noteId, userId, part, ip, sessionId);
                if (error != null) {
                    req.setAttribute("error", error);
                    resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId + "&uploadError=" +
                            java.net.URLEncoder.encode(error, "UTF-8"));
                    return;
                }
            }
        }
        resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId + "&uploaded=true");
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
