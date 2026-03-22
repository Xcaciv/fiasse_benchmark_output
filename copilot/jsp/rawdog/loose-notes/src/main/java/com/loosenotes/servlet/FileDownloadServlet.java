package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;

@WebServlet("/files")
public class FileDownloadServlet extends HttpServlet {

    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int attachmentId = Integer.parseInt(req.getParameter("id"));
            Attachment att = attachmentDAO.getAttachmentById(attachmentId);
            if (att == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Note note = noteDAO.getNoteById(att.getNoteId());
            if (note == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Check access
            HttpSession session = req.getSession(false);
            Integer sessionUserId = (session != null) ? (Integer) session.getAttribute("userId") : null;
            boolean isAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"));

            if (!note.isPublic() && !isAdmin && (sessionUserId == null || sessionUserId != note.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            String uploadDir = getServletContext().getRealPath("/") + "uploads";
            File file = new File(uploadDir + File.separator + att.getStoredFilename());
            if (!file.exists()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found on server.");
                return;
            }

            resp.setContentType(getServletContext().getMimeType(att.getOriginalFilename()));
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + att.getOriginalFilename() + "\"");
            resp.setContentLengthLong(file.length());

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
