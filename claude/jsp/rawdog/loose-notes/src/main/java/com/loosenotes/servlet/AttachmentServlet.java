package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.util.FileUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

@MultipartConfig(maxFileSize = 10485760, maxRequestSize = 10485760)
public class AttachmentServlet extends HttpServlet {

    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("download".equals(action)) {
            handleDownload(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "";

        switch (action) {
            case "upload":
                handleUpload(request, response);
                break;
            case "delete":
                handleDelete(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/dashboard");
                break;
        }
    }

    private void handleUpload(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        String noteIdParam = request.getParameter("noteId");
        if (noteIdParam == null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            int noteId = Integer.parseInt(noteIdParam);
            Note note = noteDAO.findById(noteId);

            if (note == null) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }
            if (note.getUserId() != userId && !"ADMIN".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&error=No+file+selected");
                return;
            }

            String originalFilename = FileUtil.sanitizeFilename(filePart.getSubmittedFileName());
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = "unnamed_file";
            }

            String storedFilename = FileUtil.generateStoredFilename(originalFilename);
            String contentType = filePart.getContentType();
            long fileSize = filePart.getSize();

            try (InputStream inputStream = filePart.getInputStream()) {
                if (!FileUtil.saveFile(inputStream, storedFilename)) {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&error=Failed+to+save+file");
                    return;
                }
            }

            attachmentDAO.create(noteId, originalFilename, storedFilename, fileSize, contentType);
            activityLogDAO.log(userId, "UPLOAD_ATTACHMENT", "Uploaded file: " + originalFilename + " to note id=" + noteId);

            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&success=File+uploaded+successfully");

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Upload attachment error", e);
            response.sendRedirect(request.getContextPath() + "/dashboard?error=Upload+failed");
        }
    }

    private void handleDownload(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        if (idParam == null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            int attachmentId = Integer.parseInt(idParam);
            Attachment attachment = attachmentDAO.findById(attachmentId);

            if (attachment == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Attachment not found.");
                return;
            }

            // Check if user can access the note this attachment belongs to
            HttpSession session = request.getSession(false);
            int userId = session != null && session.getAttribute("userId") != null
                    ? (Integer) session.getAttribute("userId") : -1;
            String role = session != null ? (String) session.getAttribute("userRole") : null;

            Note note = noteDAO.findById(attachment.getNoteId());
            if (note == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Note not found.");
                return;
            }

            if (!note.isPublic() && note.getUserId() != userId && !"ADMIN".equals(role)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            if (!FileUtil.fileExists(attachment.getStoredFilename())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found on disk.");
                return;
            }

            String contentType = attachment.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }

            response.setContentType(contentType);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + attachment.getOriginalFilename() + "\"");
            response.setContentLengthLong(attachment.getFileSize());

            FileUtil.streamFile(attachment.getStoredFilename(), response.getOutputStream());

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Download attachment error", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to download file.");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        String idParam = request.getParameter("id");
        String noteIdParam = request.getParameter("noteId");

        if (idParam == null || noteIdParam == null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            int attachmentId = Integer.parseInt(idParam);
            int noteId = Integer.parseInt(noteIdParam);

            Attachment attachment = attachmentDAO.findById(attachmentId);
            if (attachment == null) {
                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&error=Attachment+not+found");
                return;
            }

            Note note = noteDAO.findById(noteId);
            if (note == null || (note.getUserId() != userId && !"ADMIN".equals(role))) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            FileUtil.deleteFile(attachment.getStoredFilename());
            attachmentDAO.delete(attachmentId);
            activityLogDAO.log(userId, "DELETE_ATTACHMENT", "Deleted attachment: " + attachment.getOriginalFilename());

            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&success=Attachment+deleted");

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Delete attachment error", e);
            response.sendRedirect(request.getContextPath() + "/dashboard?error=Delete+failed");
        }
    }
}
