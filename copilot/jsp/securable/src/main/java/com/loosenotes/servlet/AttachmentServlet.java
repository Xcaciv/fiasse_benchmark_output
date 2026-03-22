package com.loosenotes.servlet;

import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.service.NoteService;
import com.loosenotes.service.ServiceException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * File attachment upload and download (REQ-005).
 * Upload: POST /attachments/upload?noteId={id}
 * Download: GET /attachments/{id}/download
 * Delete: POST /attachments/{id}/delete
 */
public final class AttachmentServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AttachmentServlet.class);
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private NoteService noteService;

    @Override
    public void init() {
        this.noteService = (NoteService) getServletContext().getAttribute("noteService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.matches("/\\d+/download")) {
            long attachId = extractId(pathInfo);
            downloadAttachment(req, resp, attachId);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/upload".equals(pathInfo)) {
            handleUpload(req, resp);
        } else if (pathInfo != null && pathInfo.matches("/\\d+/delete")) {
            deleteAttachment(req, resp, extractId(pathInfo));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleUpload(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!ServletFileUpload.isMultipartContent(req)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Multipart required");
            return;
        }

        long noteId = parseLong(req.getParameter("noteId"), -1);
        if (noteId < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid noteId");
            return;
        }
        long userId = getSessionUserId(req);

        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(MAX_SIZE_BYTES);

        try {
            List<FileItem> items = upload.parseRequest(req);
            for (FileItem item : items) {
                if (!item.isFormField() && item.getSize() > 0) {
                    String filename = item.getName();
                    try (InputStream is = item.getInputStream()) {
                        noteService.addAttachment(noteId, userId, filename, is, item.getSize());
                    }
                }
            }
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId + "/edit");
        } catch (FileUploadException | ServiceException e) {
            log.warn("Upload failed: {}", e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void downloadAttachment(HttpServletRequest req, HttpServletResponse resp,
                                    long attachId) throws IOException {
        Optional<Attachment> attachOpt = noteService.findAttachment(attachId);
        if (attachOpt.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Attachment a = attachOpt.get();
        // Verify the user can see the note the attachment belongs to
        Optional<Note> noteOpt = noteService.findById(a.getNoteId());
        if (noteOpt.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Note note = noteOpt.get();
        long userId = getSessionUserId(req);
        if (!note.isPublic() && note.getUserId() != userId) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        resp.setContentType(a.getMimeType());
        resp.setHeader("Content-Disposition",
                "attachment; filename=\"" + a.getOriginalFilename() + "\"");
        resp.setContentLengthLong(a.getFileSize());

        try {
            noteService.findAttachment(attachId); // already verified
            // Stream file to response
            noteService.getAttachments(a.getNoteId()); // no-op, use service
        } catch (Exception e) {
            log.error("Download failed for attachment {}", attachId, e);
        }
        // Delegate streaming to NoteService via FileService
        getServletContext().getAttribute("fileService");
        com.loosenotes.service.FileService fileService =
                (com.loosenotes.service.FileService) getServletContext().getAttribute("fileService");
        fileService.retrieve(a.getStoredFilename(), resp.getOutputStream());
    }

    private void deleteAttachment(HttpServletRequest req, HttpServletResponse resp,
                                   long attachId) throws IOException {
        long userId = getSessionUserId(req);
        String role  = (String) req.getSession().getAttribute("userRole");
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

        Optional<Attachment> attachOpt = noteService.findAttachment(attachId);
        if (attachOpt.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long noteId = attachOpt.get().getNoteId();

        try {
            noteService.deleteAttachment(attachId, userId, isAdmin);
            resp.sendRedirect(req.getContextPath() + "/notes/" + noteId + "/edit");
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    private long getSessionUserId(HttpServletRequest req) {
        return (Long) req.getSession().getAttribute("userId");
    }

    private long extractId(String pathInfo) {
        String[] parts = pathInfo.split("/");
        return Long.parseLong(parts[1]);
    }

    private long parseLong(String s, long defaultVal) {
        if (s == null) return defaultVal;
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return defaultVal; }
    }
}
