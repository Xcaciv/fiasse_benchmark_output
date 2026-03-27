package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.dao.DatabaseUtil;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/files/*")
public class FileDownloadServlet extends HttpServlet {

    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) { resp.sendError(400); return; }
        int attachmentId;
        try {
            attachmentId = Integer.parseInt(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            resp.sendError(400); return;
        }

        Attachment attachment = attachmentDAO.findById(attachmentId);
        if (attachment == null) { resp.sendError(404); return; }

        Note note = noteDAO.findById(attachment.getNoteId());
        if (note == null) { resp.sendError(404); return; }

        // Check access
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String shareToken = req.getParameter("token");

        boolean canAccess = false;
        if (currentUser != null) {
            canAccess = note.isPublic()
                || note.getUserId() == currentUser.getId()
                || currentUser.isAdmin();
        }
        if (!canAccess && shareToken != null) {
            ShareLink link = shareLinkDAO.findByToken(shareToken);
            canAccess = (link != null && link.getNoteId() == note.getId());
        }
        if (!canAccess) { resp.sendError(403); return; }

        File file = new File(DatabaseUtil.getUploadDir(), attachment.getStoredFilename());
        if (!file.exists()) { resp.sendError(404, "File not found on server."); return; }

        resp.setContentType(getContentType(attachment.getOriginalFilename()));
        resp.setHeader("Content-Disposition",
            "attachment; filename=\"" + URLEncoder.encode(attachment.getOriginalFilename(), StandardCharsets.UTF_8) + "\"");
        resp.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }
    }

    private String getContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
