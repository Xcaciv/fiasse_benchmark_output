package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.util.FileUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-005 – Attachment upload and download.
 *
 * GET  /attachments/download?id=N  – download a file
 * POST /attachments/upload         – upload a file to an existing note
 * POST /attachments/delete         – delete a single attachment
 *
 * SSEM: Authorization (note access check), Secure Storage (UUID names, path traversal prevention).
 */
public class AttachmentServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AttachmentServlet.class.getName());
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String action = req.getPathInfo(); // e.g. "/download"
        if ("/download".equals(action)) {
            handleDownload(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String action = req.getPathInfo();
        if ("/upload".equals(action)) {
            handleUpload(req, res);
        } else if ("/delete".equals(action)) {
            handleDelete(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ── Download ──────────────────────────────────────────────────────────

    private void handleDownload(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        long attachId = ValidationUtil.parseLong(req.getParameter("id"));
        long userId   = getSessionUserId(req);

        try {
            Attachment a = attachmentDAO.findById(attachId);
            if (a == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

            // Verify the caller may access the parent note
            Note note = noteDAO.findById(a.getNoteId());
            if (note == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            if (!note.isPublic() && note.getUserId() != userId) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN); return;
            }

            File file = FileUtil.resolve(a.getStoredFilename());
            if (file == null || !file.exists()) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND); return;
            }

            // Safe original filename for Content-Disposition
            String safeOriginal = a.getOriginalFilename().replaceAll("[^a-zA-Z0-9._\\-]", "_");
            res.setContentType("application/octet-stream");
            res.setHeader("Content-Disposition", "attachment; filename=\"" + safeOriginal + "\"");
            res.setContentLengthLong(file.length());

            try (InputStream in = new FileInputStream(file);
                 OutputStream out = res.getOutputStream()) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error downloading attachment " + attachId, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ── Upload ────────────────────────────────────────────────────────────

    private void handleUpload(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long noteId = ValidationUtil.parseLong(req.getParameter("noteId"));
        long userId = getSessionUserId(req);

        try {
            Note note = noteDAO.findById(noteId);
            if (note == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            if (note.getUserId() != userId) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

            Part filePart = req.getPart("attachment");
            if (filePart == null || filePart.getSize() == 0) {
                res.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId + "&error=nofile");
                return;
            }

            String origName = extractFilename(filePart);
            String fileError = FileUtil.validateFilename(origName);
            if (fileError == null && filePart.getSize() > FileUtil.MAX_FILE_SIZE) {
                fileError = "File exceeds 10 MB limit.";
            }
            if (fileError != null) {
                LOGGER.warning("Rejected upload from user " + userId + ": " + fileError);
                res.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId
                        + "&error=" + java.net.URLEncoder.encode(fileError, "UTF-8"));
                return;
            }

            String stored = FileUtil.store(filePart.getInputStream(), origName);
            attachmentDAO.create(noteId, origName, stored, filePart.getSize());
            res.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Attachment upload error for note " + noteId, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────

    private void handleDelete(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        long attachId = ValidationUtil.parseLong(req.getParameter("attachmentId"));
        long userId   = getSessionUserId(req);

        try {
            Attachment a = attachmentDAO.findById(attachId);
            if (a == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

            Note note = noteDAO.findById(a.getNoteId());
            if (note == null || note.getUserId() != userId) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN); return;
            }

            FileUtil.delete(a.getStoredFilename());
            attachmentDAO.delete(attachId);
            res.sendRedirect(req.getContextPath() + "/notes/view?id=" + a.getNoteId());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Attachment delete error", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private long getSessionUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return (session != null && session.getAttribute("userId") != null)
                ? (long) session.getAttribute("userId") : -1;
    }

    private String extractFilename(Part part) {
        String header = part.getHeader("content-disposition");
        if (header == null) return "upload";
        for (String token : header.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return "upload";
    }
}
