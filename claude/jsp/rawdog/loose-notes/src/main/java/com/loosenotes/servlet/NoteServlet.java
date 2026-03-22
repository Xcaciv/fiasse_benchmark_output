package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.FileUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class NoteServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "view";

        switch (action) {
            case "create":
                handleCreateForm(request, response);
                break;
            case "edit":
                handleEditForm(request, response);
                break;
            case "view":
            default:
                handleView(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "";

        switch (action) {
            case "create":
                handleCreate(request, response);
                break;
            case "edit":
                handleUpdate(request, response);
                break;
            case "delete":
                handleDelete(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/dashboard");
                break;
        }
    }

    private void handleCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/jsp/createNote.jsp").forward(request, response);
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");

        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String isPublicParam = request.getParameter("isPublic");
        boolean isPublic = "on".equals(isPublicParam) || "true".equals(isPublicParam) || "1".equals(isPublicParam);

        if (title == null || title.trim().isEmpty()) {
            request.setAttribute("error", "Title is required.");
            request.setAttribute("content", content);
            request.setAttribute("isPublic", isPublic);
            request.getRequestDispatcher("/WEB-INF/jsp/createNote.jsp").forward(request, response);
            return;
        }
        if (content == null || content.trim().isEmpty()) {
            request.setAttribute("error", "Content is required.");
            request.setAttribute("title", title);
            request.setAttribute("isPublic", isPublic);
            request.getRequestDispatcher("/WEB-INF/jsp/createNote.jsp").forward(request, response);
            return;
        }

        try {
            int noteId = noteDAO.create(userId, title.trim(), content.trim(), isPublic);
            activityLogDAO.log(userId, "CREATE_NOTE", "Created note: " + title.trim());
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&success=Note+created+successfully");
        } catch (SQLException e) {
            getServletContext().log("Create note error", e);
            request.setAttribute("error", "Failed to create note. Please try again.");
            request.setAttribute("title", title);
            request.setAttribute("content", content);
            request.getRequestDispatcher("/WEB-INF/jsp/createNote.jsp").forward(request, response);
        }
    }

    private void handleEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        String idParam = request.getParameter("id");
        if (idParam == null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            int noteId = Integer.parseInt(idParam);
            Note note = noteDAO.findById(noteId);

            if (note == null) {
                request.setAttribute("errorMessage", "Note not found.");
                request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
                return;
            }
            if (note.getUserId() != userId && !"ADMIN".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            request.setAttribute("note", note);
            request.getRequestDispatcher("/WEB-INF/jsp/editNote.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Edit form error", e);
            request.setAttribute("errorMessage", "Failed to load note.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        String idParam = request.getParameter("id");
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String isPublicParam = request.getParameter("isPublic");
        boolean isPublic = "on".equals(isPublicParam) || "true".equals(isPublicParam) || "1".equals(isPublicParam);

        if (idParam == null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        if (title == null || title.trim().isEmpty()) {
            request.setAttribute("error", "Title is required.");
            try {
                int noteId = Integer.parseInt(idParam);
                Note note = noteDAO.findById(noteId);
                request.setAttribute("note", note);
            } catch (Exception ignored) {}
            request.getRequestDispatcher("/WEB-INF/jsp/editNote.jsp").forward(request, response);
            return;
        }

        try {
            int noteId = Integer.parseInt(idParam);
            Note note = noteDAO.findById(noteId);

            if (note == null) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }
            if (note.getUserId() != userId && !"ADMIN".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            noteDAO.update(noteId, title.trim(), content != null ? content.trim() : "", isPublic);
            activityLogDAO.log(userId, "UPDATE_NOTE", "Updated note id=" + noteId + ": " + title.trim());
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&success=Note+updated+successfully");

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Update note error", e);
            request.setAttribute("error", "Failed to update note.");
            request.getRequestDispatcher("/WEB-INF/jsp/editNote.jsp").forward(request, response);
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        String idParam = request.getParameter("id");
        if (idParam == null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            int noteId = Integer.parseInt(idParam);
            Note note = noteDAO.findById(noteId);

            if (note == null) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }
            if (note.getUserId() != userId && !"ADMIN".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            // Delete attachments from disk
            List<Attachment> attachments = attachmentDAO.findByNoteId(noteId);
            for (Attachment attachment : attachments) {
                FileUtil.deleteFile(attachment.getStoredFilename());
            }

            noteDAO.delete(noteId);
            activityLogDAO.log(userId, "DELETE_NOTE", "Deleted note id=" + noteId + ": " + note.getTitle());
            response.sendRedirect(request.getContextPath() + "/dashboard?success=Note+deleted+successfully");

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Delete note error", e);
            response.sendRedirect(request.getContextPath() + "/dashboard?error=Failed+to+delete+note");
        }
    }

    private void handleView(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = session != null && session.getAttribute("userId") != null
                ? (Integer) session.getAttribute("userId") : -1;
        String role = session != null ? (String) session.getAttribute("userRole") : null;

        String idParam = request.getParameter("id");
        if (idParam == null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            int noteId = Integer.parseInt(idParam);
            Note note = noteDAO.findById(noteId);

            if (note == null) {
                request.setAttribute("errorMessage", "Note not found.");
                request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
                return;
            }

            // Only show private notes to the owner or admin
            if (!note.isPublic() && note.getUserId() != userId && !"ADMIN".equals(role)) {
                if (userId < 0) {
                    response.sendRedirect(request.getContextPath() + "/login");
                } else {
                    request.setAttribute("errorMessage", "You don't have permission to view this note.");
                    request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
                }
                return;
            }

            List<Attachment> attachments = attachmentDAO.findByNoteId(noteId);
            List<Rating> ratings = ratingDAO.findByNoteId(noteId);
            double avgRating = ratingDAO.getAverageRating(noteId);
            int ratingCount = ratingDAO.getRatingCount(noteId);
            ShareLink shareLink = shareLinkDAO.findByNoteId(noteId);

            Rating userRating = null;
            if (userId > 0) {
                userRating = ratingDAO.findByNoteAndUser(noteId, userId);
            }

            note.setAverageRating(avgRating);
            note.setRatingCount(ratingCount);

            request.setAttribute("note", note);
            request.setAttribute("attachments", attachments);
            request.setAttribute("ratings", ratings);
            request.setAttribute("shareLink", shareLink);
            request.setAttribute("userRating", userRating);

            // Flash messages
            String success = request.getParameter("success");
            if (success != null) {
                request.setAttribute("success", success);
            }

            request.getRequestDispatcher("/WEB-INF/jsp/viewNote.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("View note error", e);
            request.setAttribute("errorMessage", "Failed to load note.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }
}
