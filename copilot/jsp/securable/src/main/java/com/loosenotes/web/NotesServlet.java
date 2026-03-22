package com.loosenotes.web;

import com.loosenotes.dao.ActivityLogDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.User;
import com.loosenotes.service.FileStorageService;
import com.loosenotes.util.AppUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet(name = "NotesServlet", urlPatterns = {"/notes", "/notes/*"})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 25 * 1024 * 1024)
public class NotesServlet extends BaseServlet {
    private static final Logger LOGGER = Logger.getLogger(NotesServlet.class.getName());
    private final NoteDao noteDao = new NoteDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = path(request);
        try {
            switch (path) {
                case "":
                case "/":
                    showMine(request, response);
                    break;
                case "/create":
                    showCreateForm(request, response);
                    break;
                case "/view":
                    showDetail(request, response);
                    break;
                case "/edit":
                    showEditForm(request, response);
                    break;
                case "/search":
                    showSearch(request, response);
                    break;
                case "/top-rated":
                    showTopRated(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = path(request);
        try {
            switch (path) {
                case "/create":
                    createNote(request, response);
                    break;
                case "/edit":
                    updateNote(request, response);
                    break;
                case "/delete":
                    deleteNote(request, response);
                    break;
                case "/share/generate":
                    generateShareLink(request, response);
                    break;
                case "/share/revoke":
                    revokeShareLink(request, response);
                    break;
                case "/rate":
                    saveRating(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    private void showMine(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        User user = requireLogin(request, response);
        if (user == null) {
            return;
        }
        request.setAttribute("notes", noteDao.listOwnerNotes(user.getId()));
        render(request, response, "notes/list.jsp", "My notes");
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        if (requireLogin(request, response) == null) {
            return;
        }
        request.setAttribute("mode", "create");
        render(request, response, "notes/form.jsp", "Create note");
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        User user = requireLogin(request, response);
        if (user == null) {
            return;
        }
        Note note = loadManagedNote(request, response, user, true);
        if (note == null) {
            return;
        }
        request.setAttribute("mode", "edit");
        request.setAttribute("note", note);
        request.setAttribute("attachments", noteDao.listAttachments(note.getId()));
        render(request, response, "notes/form.jsp", "Edit note");
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        Note note = requireVisibleNote(request, response, false);
        if (note == null) {
            return;
        }
        User user = currentUser(request);
        populateDetail(request, note, user, null, false);
        render(request, response, "notes/detail.jsp", note.getTitle());
    }

    private void showSearch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
        User user = currentUser(request);
        String q = AppUtil.trimToEmpty(request.getParameter("q"));
        request.setAttribute("query", q);
        request.setAttribute("results", noteDao.searchVisibleNotes(q, user == null ? null : user.getId()));
        render(request, response, "notes/search.jsp", "Search notes");
    }

    private void showTopRated(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
        request.setAttribute("results", noteDao.listTopRatedNotes());
        render(request, response, "notes/top-rated.jsp", "Top rated notes");
    }

    private void createNote(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        User user = requireLogin(request, response);
        if (user == null || !requireCsrf(request, response)) {
            return;
        }
        String title = AppUtil.trimToEmpty(request.getParameter("title"));
        String content = AppUtil.trimToEmpty(request.getParameter("content"));
        boolean publicNote = request.getParameter("isPublic") != null;
        List<String> errors = new ArrayList<>(AppUtil.validateNote(title, content));
        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("mode", "create");
            request.setAttribute("formTitle", title);
            request.setAttribute("formContent", content);
            request.setAttribute("formPublic", publicNote);
            render(request, response, "notes/form.jsp", "Create note");
            return;
        }
        long noteId = noteDao.createNote(user.getId(), title, content, publicNote);
        saveUploadedAttachments(request, noteId, errors);
        if (!errors.isEmpty()) {
            setFlash(request, "error", String.join(" ", errors));
        }
        activityLogDao.log(user.getId(), "note.create", "Created note #" + noteId + ".");
        setFlash(request, "success", "Note created successfully.");
        response.sendRedirect(request.getContextPath() + "/notes/view?id=" + noteId);
    }

    private void updateNote(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        User user = requireLogin(request, response);
        if (user == null || !requireCsrf(request, response)) {
            return;
        }
        Note note = loadManagedNote(request, response, user, true);
        if (note == null) {
            return;
        }
        String title = AppUtil.trimToEmpty(request.getParameter("title"));
        String content = AppUtil.trimToEmpty(request.getParameter("content"));
        boolean publicNote = request.getParameter("isPublic") != null;
        List<String> errors = new ArrayList<>(AppUtil.validateNote(title, content));
        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("mode", "edit");
            request.setAttribute("note", note);
            request.setAttribute("attachments", noteDao.listAttachments(note.getId()));
            request.setAttribute("formTitle", title);
            request.setAttribute("formContent", content);
            request.setAttribute("formPublic", publicNote);
            render(request, response, "notes/form.jsp", "Edit note");
            return;
        }
        noteDao.updateNote(note.getId(), title, content, publicNote);
        saveUploadedAttachments(request, note.getId(), errors);
        activityLogDao.log(user.getId(), "note.update", "Updated note #" + note.getId() + ".");
        if (!errors.isEmpty()) {
            setFlash(request, "error", String.join(" ", errors));
        }
        setFlash(request, "success", errors.isEmpty() ? "Note updated successfully." : "Note updated. Some attachments were skipped.");
        response.sendRedirect(request.getContextPath() + "/notes/view?id=" + note.getId());
    }

    private void deleteNote(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        User user = requireLogin(request, response);
        if (user == null || !requireCsrf(request, response)) {
            return;
        }
        long noteId = parseNoteId(request, response);
        if (noteId < 0) {
            return;
        }
        Note note = noteDao.findById(noteId).orElse(null);
        if (!canManage(note, user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        List<Attachment> attachments = noteDao.listAttachments(note.getId());
        noteDao.deleteNote(note.getId());
        for (Attachment attachment : attachments) {
            try {
                FileStorageService.deleteIfExists(attachment.getStoredName());
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Failed to remove stored attachment " + attachment.getStoredName(), ex);
            }
        }
        activityLogDao.log(user.getId(), "note.delete", "Deleted note #" + note.getId() + ".");
        setFlash(request, "success", "Note deleted.");
        response.sendRedirect(request.getContextPath() + "/notes");
    }

    private void generateShareLink(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        User user = requireLogin(request, response);
        if (user == null || !requireCsrf(request, response)) {
            return;
        }
        Note note = loadManagedNote(request, response, user, false);
        if (note == null || !isOwner(note, user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String rawToken = com.loosenotes.security.SecurityUtil.newToken();
        noteDao.revokeActiveShareLinks(note.getId());
        noteDao.createShareLink(note.getId(), com.loosenotes.security.SecurityUtil.sha256Base64(rawToken));
        activityLogDao.log(user.getId(), "note.share_generated", "Generated share link for note #" + note.getId() + ".");
        setFlash(request, "success", "Share link generated. Copy it before leaving this page.");
        setFlashValue(request, "flashShareUrl", appUrl(request, "/shared/view?token=" + rawToken));
        response.sendRedirect(request.getContextPath() + "/notes/view?id=" + note.getId());
    }

    private void revokeShareLink(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        User user = requireLogin(request, response);
        if (user == null || !requireCsrf(request, response)) {
            return;
        }
        Note note = loadManagedNote(request, response, user, false);
        if (note == null || !isOwner(note, user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        noteDao.revokeActiveShareLinks(note.getId());
        activityLogDao.log(user.getId(), "note.share_revoked", "Revoked share link for note #" + note.getId() + ".");
        setFlash(request, "success", "Active share link revoked.");
        response.sendRedirect(request.getContextPath() + "/notes/view?id=" + note.getId());
    }

    private void saveRating(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        User user = requireLogin(request, response);
        if (user == null || !requireCsrf(request, response)) {
            return;
        }
        long noteId = parseNoteId(request, response);
        if (noteId < 0) {
            return;
        }
        Note note = noteDao.findById(noteId).orElse(null);
        if (!canView(note, user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int ratingValue;
        try {
            ratingValue = Integer.parseInt(request.getParameter("ratingValue"));
        } catch (NumberFormatException ex) {
            ratingValue = 0;
        }
        String comment = AppUtil.trimToEmpty(request.getParameter("comment"));
        String shareToken = AppUtil.trimToEmpty(request.getParameter("shareToken"));
        boolean shareAccess = !shareToken.isBlank()
            && noteDao.shareTokenGrantsAccess(noteId, com.loosenotes.security.SecurityUtil.sha256Base64(shareToken));
        if (!canView(note, user) && !shareAccess) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (ratingValue < 1 || ratingValue > 5 || comment.length() > 500) {
            request.setAttribute("errors", List.of("Ratings must be between 1 and 5 stars and comments must be 500 characters or fewer."));
            populateDetail(request, note, user, shareAccess ? shareToken : null, shareAccess);
            render(request, response, "notes/detail.jsp", note.getTitle());
            return;
        }
        noteDao.upsertRating(noteId, user.getId(), ratingValue, comment);
        activityLogDao.log(user.getId(), "note.rate", "Saved rating for note #" + noteId + ".");
        setFlash(request, "success", "Your rating has been saved.");
        if (shareAccess) {
            response.sendRedirect(request.getContextPath() + "/shared/view?token=" + shareToken);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/notes/view?id=" + noteId);
    }

    private void saveUploadedAttachments(HttpServletRequest request, long noteId, List<String> errors) throws IOException, ServletException, SQLException {
        for (Part part : request.getParts()) {
            if (!"attachments".equals(part.getName()) || part.getSize() == 0) {
                continue;
            }
            try {
                FileStorageService.StoredFile storedFile = FileStorageService.store(part);
                if (storedFile != null) {
                    noteDao.addAttachment(noteId, storedFile.getStoredName(), storedFile.getOriginalName(), storedFile.getContentType(), storedFile.getSizeBytes());
                }
            } catch (IOException ex) {
                errors.add(ex.getMessage());
            }
        }
    }

    private void populateDetail(HttpServletRequest request, Note note, User user, String shareToken, boolean sharedView) throws SQLException {
        request.setAttribute("note", note);
        request.setAttribute("attachments", noteDao.listAttachments(note.getId()));
        request.setAttribute("ratings", noteDao.listRatings(note.getId()));
        request.setAttribute("myRating", user == null ? null : noteDao.findUserRating(note.getId(), user.getId()).orElse(null));
        request.setAttribute("activeShareLink", noteDao.findActiveShareLinkForNote(note.getId()).orElse(null));
        request.setAttribute("shareMode", sharedView);
        request.setAttribute("shareToken", shareToken);
        if (user != null && user.isAdmin()) {
            request.setAttribute("assignableUsers", userDao.listAssignableUsers());
        }
    }

    private Note requireVisibleNote(HttpServletRequest request, HttpServletResponse response, boolean ownerOnly) throws IOException, SQLException {
        long noteId = parseNoteId(request, response);
        if (noteId < 0) {
            return null;
        }
        Note note = noteDao.findById(noteId).orElse(null);
        User user = currentUser(request);
        if (note == null || (ownerOnly ? !isOwner(note, user) : !canView(note, user))) {
            response.sendError(note == null ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return note;
    }

    private Note loadManagedNote(HttpServletRequest request, HttpServletResponse response, User user, boolean ownerOnly) throws IOException, SQLException {
        long noteId = parseNoteId(request, response);
        if (noteId < 0) {
            return null;
        }
        Note note = noteDao.findById(noteId).orElse(null);
        if (note == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        boolean allowed = ownerOnly ? isOwner(note, user) : canManage(note, user);
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return note;
    }

    private long parseNoteId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            return Long.parseLong(request.getParameter("id"));
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A valid note id is required.");
            return -1;
        }
    }

    private String path(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null ? "" : path;
    }
}
