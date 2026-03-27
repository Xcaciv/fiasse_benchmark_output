package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.User;
import com.loosenotes.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.List;

public class NoteServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(NoteServlet.class);
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo(); // e.g. null, "/create", "/5", "/5/edit", "/5/delete"

        if (pathInfo == null || pathInfo.equals("/")) {
            handleList(req, resp);
        } else if (pathInfo.equals("/create")) {
            req.getRequestDispatcher("/WEB-INF/views/notes/create.jsp").forward(req, resp);
        } else {
            String[] parts = pathInfo.split("/");
            // parts[0] = "", parts[1] = id, parts[2] (optional) = action
            try {
                int noteId = Integer.parseInt(parts[1]);
                String action = parts.length > 2 ? parts[2] : "view";
                switch (action) {
                    case "edit" -> handleEditGet(req, resp, noteId);
                    case "delete" -> handleDeleteGet(req, resp, noteId);
                    default -> handleDetails(req, resp, noteId);
                }
            } catch (NumberFormatException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            resp.sendRedirect(req.getContextPath() + "/notes");
            return;
        }

        if (pathInfo.equals("/create")) {
            handleCreate(req, resp);
            return;
        }

        String[] parts = pathInfo.split("/");
        try {
            int noteId = Integer.parseInt(parts[1]);
            String action = parts.length > 2 ? parts[2] : "";
            switch (action) {
                case "edit" -> handleEditPost(req, resp, noteId);
                case "delete" -> handleDelete(req, resp, noteId);
                default -> resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(req);
        List<Note> notes = noteDAO.findByUserId(currentUser.getId());
        req.setAttribute("notes", notes);
        req.getRequestDispatcher("/WEB-INF/views/notes/index.jsp").forward(req, resp);
    }

    private void handleDetails(HttpServletRequest req, HttpServletResponse resp, int noteId)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(req);
        Note note = noteDAO.findById(noteId);

        if (note == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Access check: owner can always view, others can only view public notes
        if (note.getUserId() != currentUser.getId() && !note.isPublic() && !currentUser.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        List<Attachment> attachments = attachmentDAO.findByNoteId(noteId);
        List<Rating> ratings = ratingDAO.findByNoteId(noteId);
        Rating userRating = ratingDAO.findByNoteAndUser(noteId, currentUser.getId());

        com.loosenotes.dao.ShareLinkDAO shareLinkDAO = new com.loosenotes.dao.ShareLinkDAO();
        com.loosenotes.model.ShareLink shareLink = shareLinkDAO.findByNoteId(noteId);

        req.setAttribute("note", note);
        req.setAttribute("attachments", attachments);
        req.setAttribute("ratings", ratings);
        req.setAttribute("userRating", userRating);
        req.setAttribute("existingShareLink", shareLink);
        req.getRequestDispatcher("/WEB-INF/views/notes/details.jsp").forward(req, resp);
    }

    private void handleCreate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(req);
        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String isPublicStr = req.getParameter("isPublic");

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            req.setAttribute("error", "Title and content are required.");
            req.getRequestDispatcher("/WEB-INF/views/notes/create.jsp").forward(req, resp);
            return;
        }

        Note note = new Note();
        note.setTitle(title.trim());
        note.setContent(content.trim());
        note.setUserId(currentUser.getId());
        note.setPublic("on".equals(isPublicStr) || "true".equals(isPublicStr));

        if (!noteDAO.create(note)) {
            req.setAttribute("error", "Failed to create note. Please try again.");
            req.getRequestDispatcher("/WEB-INF/views/notes/create.jsp").forward(req, resp);
            return;
        }

        // Handle file upload
        try {
            Part filePart = req.getPart("attachment");
            if (filePart != null && filePart.getSize() > 0) {
                String originalFilename = getSubmittedFileName(filePart);
                if (originalFilename != null && !originalFilename.isBlank()) {
                    saveAttachment(filePart, originalFilename, note.getId(), req, resp);
                }
            }
        } catch (Exception e) {
            logger.warn("File upload failed for new note {}: {}", note.getId(), e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/notes/" + note.getId());
    }

    private void handleEditGet(HttpServletRequest req, HttpServletResponse resp, int noteId)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(req);
        Note note = noteDAO.findById(noteId);

        if (note == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        List<Attachment> attachments = attachmentDAO.findByNoteId(noteId);
        req.setAttribute("note", note);
        req.setAttribute("attachments", attachments);
        req.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(req, resp);
    }

    private void handleEditPost(HttpServletRequest req, HttpServletResponse resp, int noteId)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(req);
        Note note = noteDAO.findById(noteId);

        if (note == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String isPublicStr = req.getParameter("isPublic");

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            req.setAttribute("error", "Title and content are required.");
            req.setAttribute("note", note);
            req.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(req, resp);
            return;
        }

        note.setTitle(title.trim());
        note.setContent(content.trim());
        note.setPublic("on".equals(isPublicStr) || "true".equals(isPublicStr));

        if (!noteDAO.update(note)) {
            req.setAttribute("error", "Failed to update note.");
            req.setAttribute("note", note);
            req.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(req, resp);
            return;
        }

        // Handle additional file upload
        try {
            Part filePart = req.getPart("attachment");
            if (filePart != null && filePart.getSize() > 0) {
                String originalFilename = getSubmittedFileName(filePart);
                if (originalFilename != null && !originalFilename.isBlank()) {
                    saveAttachment(filePart, originalFilename, noteId, req, resp);
                }
            }
        } catch (Exception e) {
            logger.warn("File upload failed during note edit {}: {}", noteId, e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/notes/" + noteId);
    }

    private void handleDeleteGet(HttpServletRequest req, HttpServletResponse resp, int noteId)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(req);
        Note note = noteDAO.findById(noteId);

        if (note == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        req.setAttribute("note", note);
        req.getRequestDispatcher("/WEB-INF/views/notes/delete.jsp").forward(req, resp);
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, int noteId)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(req);
        Note note = noteDAO.findById(noteId);

        if (note == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Delete stored files first
        List<Attachment> attachments = attachmentDAO.findByNoteId(noteId);
        for (Attachment a : attachments) {
            FileUtil.deleteFile(a.getStoredFilename());
        }

        noteDAO.delete(noteId);
        resp.sendRedirect(req.getContextPath() + "/notes");
    }

    private void saveAttachment(Part filePart, String originalFilename, int noteId,
                                 HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!FileUtil.isAllowedExtension(originalFilename)) {
            logger.warn("Rejected file upload with disallowed extension: {}", originalFilename);
            return;
        }
        if (!FileUtil.isWithinSizeLimit(filePart.getSize())) {
            logger.warn("Rejected file upload exceeding size limit: {} bytes", filePart.getSize());
            return;
        }
        String storedFilename = FileUtil.generateStoredFilename(originalFilename);
        FileUtil.saveFile(filePart.getInputStream(), storedFilename);

        Attachment attachment = new Attachment();
        attachment.setNoteId(noteId);
        attachment.setOriginalFilename(originalFilename);
        attachment.setStoredFilename(storedFilename);
        attachment.setFileSize(filePart.getSize());
        attachmentDAO.create(attachment);
    }

    private String getSubmittedFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) return null;
        for (String token : contentDisposition.split(";")) {
            token = token.trim();
            if (token.startsWith("filename")) {
                String filename = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                // Handle Windows-style paths
                int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
                return lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
            }
        }
        return null;
    }

    private User getCurrentUser(HttpServletRequest req) {
        return (User) req.getSession().getAttribute("currentUser");
    }
}
