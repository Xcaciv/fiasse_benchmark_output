package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.ShareLink;
import com.loosenotes.service.AuditService;
import com.loosenotes.service.NoteService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.service.ShareTokenService;
import com.loosenotes.util.CsrfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Central controller for all note CRUD and sharing operations under {@code /notes/*}.
 *
 * <p>Route dispatch is performed on {@link HttpServletRequest#getPathInfo()}.
 * Path segments are never used in file-system or SQL operations directly —
 * they are parsed as typed numeric IDs and validated before use.
 *
 * <p>All GET routes retrieve {@code userId} from the session (set by
 * {@link LoginServlet}). POST routes additionally validate the CSRF token.
 * Ownership is enforced by the service layer, not here.
 */
@WebServlet("/notes/*")
public class NoteServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(NoteServlet.class);
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_PAGE_SIZE = 10;

    private NoteService noteService;
    private ShareTokenService shareTokenService;
    private AttachmentDao attachmentDao;
    private RatingDao ratingDao;

    @Override
    public void init() throws ServletException {
        AuditService auditService = new AuditService(new AuditLogDao());
        NoteDao noteDao = new NoteDao();

        noteService = new NoteService(noteDao, auditService);
        shareTokenService = new ShareTokenService(
                new ShareLinkDao(), noteDao, auditService);
        attachmentDao = new AttachmentDao();
        ratingDao     = new RatingDao();
    }

    // =========================================================================
    // GET dispatch
    // =========================================================================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            String pathInfo = request.getPathInfo();   // null or "/", "/create", "/{id}", etc.

            if (pathInfo == null || pathInfo.equals("/")) {
                handleListNotes(request, response);
                return;
            }

            String[] parts = pathInfo.split("/");
            // parts[0] == "" (leading slash), parts[1] == first segment

            if (parts.length == 2) {
                String segment = parts[1];
                if ("create".equals(segment)) {
                    handleCreateForm(request, response);
                } else {
                    Long noteId = parseId(segment);
                    if (noteId == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    handleViewNote(request, response, noteId);
                }
            } else if (parts.length == 3) {
                Long noteId = parseId(parts[1]);
                if (noteId == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                String action = parts[2];
                if ("edit".equals(action)) {
                    handleEditForm(request, response, noteId);
                } else if ("delete".equals(action)) {
                    handleDeleteConfirm(request, response, noteId);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // POST dispatch
    // =========================================================================

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String[] parts = pathInfo.split("/");

            if (parts.length == 2 && "create".equals(parts[1])) {
                handleCreateNote(request, response);
                return;
            }

            if (parts.length >= 3) {
                Long noteId = parseId(parts[1]);
                if (noteId == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                String action = parts[2];

                if ("edit".equals(action)) {
                    handleUpdateNote(request, response, noteId);
                } else if ("delete".equals(action)) {
                    handleDeleteNote(request, response, noteId);
                } else if ("share".equals(action)) {
                    if (parts.length == 5 && "revoke".equals(parts[4])) {
                        Long linkId = parseId(parts[3]);
                        if (linkId == null) {
                            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                            return;
                        }
                        handleRevokeShareLink(request, response, noteId, linkId);
                    } else if (parts.length == 3) {
                        handleCreateShareLink(request, response, noteId);
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }

            response.sendError(HttpServletResponse.SC_NOT_FOUND);

        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // Route handlers — GET
    // =========================================================================

    private void handleListNotes(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Long userId = getRequiredUserId(request, response);
        if (userId == null) return;

        int page = parsePage(request);
        List<Note> notes = noteService.getUserNotes(userId, page, DEFAULT_PAGE_SIZE);

        request.setAttribute("notes", notes);
        request.setAttribute("page", page);
        request.getRequestDispatcher("/WEB-INF/jsp/note/list.jsp").forward(request, response);
    }

    private void handleCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Long userId = getRequiredUserId(request, response);
        if (userId == null) return;

        HttpSession session = request.getSession(false);
        request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
        request.getRequestDispatcher("/WEB-INF/jsp/note/create.jsp").forward(request, response);
    }

    private void handleViewNote(HttpServletRequest request, HttpServletResponse response,
                                Long noteId) throws ServletException, IOException {

        // userId may be null for public notes.
        HttpSession session = request.getSession(false);
        Long userId = session != null ? (Long) session.getAttribute("userId") : null;

        try {
            Note note = noteService.getNote(noteId, userId);
            List<Attachment> attachments = attachmentDao.findByNoteId(noteId);
            List<Rating> ratings = ratingDao.findByNoteId(noteId, 1, 50);
            List<ShareLink> shareLinks = null;

            // Only the note owner can see share links.
            if (userId != null && userId.equals(note.getUserId())) {
                try {
                    shareLinks = shareTokenService.getActiveLinksForNote(noteId, userId);
                } catch (ServiceException e) {
                    log.warn("Could not load share links. noteId={} userId={}", noteId, userId);
                }
            }

            request.setAttribute("note", note);
            request.setAttribute("attachments", attachments);
            request.setAttribute("ratings", ratings);
            request.setAttribute("shareLinks", shareLinks);
            request.getRequestDispatcher("/WEB-INF/jsp/note/view.jsp").forward(request, response);

        } catch (ServiceException e) {
            handleServiceException(e, response);
        }
    }

    private void handleEditForm(HttpServletRequest request, HttpServletResponse response,
                                Long noteId) throws ServletException, IOException {

        Long userId = getRequiredUserId(request, response);
        if (userId == null) return;

        try {
            Note note = noteService.getNoteForEdit(noteId, userId);
            HttpSession session = request.getSession(false);
            request.setAttribute("note", note);
            request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher("/WEB-INF/jsp/note/edit.jsp").forward(request, response);
        } catch (ServiceException e) {
            handleServiceException(e, response);
        }
    }

    private void handleDeleteConfirm(HttpServletRequest request, HttpServletResponse response,
                                     Long noteId) throws ServletException, IOException {

        Long userId = getRequiredUserId(request, response);
        if (userId == null) return;

        try {
            Note note = noteService.getNoteForEdit(noteId, userId);
            HttpSession session = request.getSession(false);
            request.setAttribute("note", note);
            request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher("/WEB-INF/jsp/note/delete.jsp").forward(request, response);
        } catch (ServiceException e) {
            handleServiceException(e, response);
        }
    }

    // =========================================================================
    // Route handlers — POST
    // =========================================================================

    private void handleCreateNote(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Long userId = getRequiredUserId(request, response);
        if (userId == null) return;

        HttpSession session = request.getSession(false);
        if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String title      = request.getParameter("title");
        String content    = request.getParameter("content");
        String visParam   = request.getParameter("visibility");
        Note.Visibility visibility = parseVisibility(visParam);

        try {
            Note created = noteService.createNote(userId, title, content, visibility);
            CsrfUtil.rotateToken(session);
            response.sendRedirect(request.getContextPath() + "/notes");
        } catch (ServiceException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher("/WEB-INF/jsp/note/create.jsp").forward(request, response);
        }
    }

    private void handleUpdateNote(HttpServletRequest request, HttpServletResponse response,
                                  Long noteId) throws ServletException, IOException {

        Long userId = getRequiredUserId(request, response);
        if (userId == null) return;

        HttpSession session = request.getSession(false);
        if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String title     = request.getParameter("title");
        String content   = request.getParameter("content");
        String visParam  = request.getParameter("visibility");
        Note.Visibility visibility = parseVisibility(visParam);

        try {
            noteService.updateNote(noteId, userId, title, content, visibility);
            CsrfUtil.rotateToken(session);
            response.sendRedirect(request.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException e) {
            if ("ACCESS_DENIED".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else if ("NOT_FOUND".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                Note note = new Note();
                note.setId(noteId);
                note.setTitle(title);
                note.setContent(content);
                note.setVisibility(visibility);
                request.setAttribute("note", note);
                request.setAttribute("error", e.getMessage());
                request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
                request.getRequestDispatcher("/WEB-INF/jsp/note/edit.jsp").forward(request, response);
            }
        }
    }

    private void handleDeleteNote(HttpServletRequest request, HttpServletResponse response,
                                  Long noteId) throws ServletException, IOException {

        Long userId = getRequiredUserId(request, response);
        if (userId == null) return;

        HttpSession session = request.getSession(false);
        if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String userRole = (String) session.getAttribute("userRole");
        boolean isAdmin = "ADMIN".equals(userRole);

        try {
            noteService.deleteNote(noteId, userId, isAdmin);
            CsrfUtil.rotateToken(session);
            response.sendRedirect(request.getContextPath() + "/notes");
        } catch (ServiceException e) {
            handleServiceException(e, response);
        }
    }

    private void handleCreateShareLink(HttpServletRequest request, HttpServletResponse response,
                                       Long noteId) throws ServletException, IOException {

        Long userId = getRequiredUserId(request, response);
        if (userId == null) return;

        HttpSession session = request.getSession(false);
        if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        try {
            String rawToken = shareTokenService.createShareLink(noteId, userId, request.getRemoteAddr());
            // NOTE: In production the raw token would be emailed or shown once to the user.
            // Here we include it as a flash message parameter.
            CsrfUtil.rotateToken(session);
            session.setAttribute("flashShareToken", rawToken);
            response.sendRedirect(request.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException e) {
            handleServiceException(e, response);
        }
    }

    private void handleRevokeShareLink(HttpServletRequest request, HttpServletResponse response,
                                       Long noteId, Long linkId) throws ServletException, IOException {

        Long userId = getRequiredUserId(request, response);
        if (userId == null) return;

        HttpSession session = request.getSession(false);
        if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        try {
            shareTokenService.revokeShareLink(linkId, noteId, userId);
            CsrfUtil.rotateToken(session);
            response.sendRedirect(request.getContextPath() + "/notes/" + noteId);
        } catch (ServiceException e) {
            handleServiceException(e, response);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns the {@code userId} from the session, or redirects to login and
     * returns {@code null} if the session does not carry one.
     */
    private Long getRequiredUserId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            if (userId != null) return userId;
        }
        response.sendRedirect(request.getContextPath() + "/auth/login");
        return null;
    }

    private int parsePage(HttpServletRequest request) {
        try {
            int p = Integer.parseInt(request.getParameter("page"));
            return p > 0 ? p : 1;
        } catch (NumberFormatException | NullPointerException e) {
            return 1;
        }
    }

    private Long parseId(String segment) {
        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Note.Visibility parseVisibility(String param) {
        if ("PUBLIC".equalsIgnoreCase(param)) return Note.Visibility.PUBLIC;
        return Note.Visibility.PRIVATE;
    }

    private void handleServiceException(ServiceException e, HttpServletResponse response)
            throws IOException {
        if ("ACCESS_DENIED".equals(e.getCode())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } else if ("NOT_FOUND".equals(e.getCode())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
