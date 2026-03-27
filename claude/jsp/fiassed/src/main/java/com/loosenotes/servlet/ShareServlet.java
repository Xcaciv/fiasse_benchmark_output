package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.service.AuditService;
import com.loosenotes.service.FileService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.service.ShareTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Handles public (unauthenticated) share-link access at {@code /share/*}.
 *
 * <p>Routes:
 * <ul>
 *   <li>{@code GET /share/{token}}                    — view a shared note</li>
 *   <li>{@code GET /share/{token}/attachment/{id}}    — download an attachment
 *       via share link (no session required)</li>
 * </ul>
 *
 * <p>No session mutation occurs here. Authentication is based solely on the
 * share token, which is hashed immediately on receipt by
 * {@link ShareTokenService#resolveShareLink}.
 *
 * <p>The note owner's user ID and other internal fields are present on the
 * {@link Note} object but the JSP must take care not to render them.
 */
@WebServlet("/share/*")
public class ShareServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ShareServlet.class);
    private static final long serialVersionUID = 1L;

    private ShareTokenService shareTokenService;
    private AttachmentDao attachmentDao;
    private FileService fileService;

    @Override
    public void init() throws ServletException {
        AuditService auditService = new AuditService(new AuditLogDao());
        NoteDao noteDao = new NoteDao();

        shareTokenService = new ShareTokenService(new ShareLinkDao(), noteDao, auditService);
        attachmentDao     = new AttachmentDao();
        fileService       = new FileService(attachmentDao, auditService);
    }

    // =========================================================================
    // GET dispatch
    // =========================================================================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Strip leading slash and split.
            String[] parts = pathInfo.split("/");
            // parts[0] == "" (leading "/"), parts[1] == token

            if (parts.length < 2) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String token = parts[1];

            if (parts.length == 2) {
                // GET /share/{token}
                handleShareView(request, response, token);
            } else if (parts.length == 4 && "attachment".equals(parts[2])) {
                // GET /share/{token}/attachment/{attachmentId}
                Long attachmentId = parseId(parts[3]);
                if (attachmentId == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                handleShareAttachment(request, response, token, attachmentId);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // Route handlers
    // =========================================================================

    /**
     * Resolves the share token and renders the shared note view.
     * No session is created or modified.
     */
    private void handleShareView(HttpServletRequest request, HttpServletResponse response,
                                 String token) throws ServletException, IOException {

        String ipAddress = request.getRemoteAddr();

        try {
            Note note = shareTokenService.resolveShareLink(token, ipAddress);
            List<Attachment> attachments = attachmentDao.findByNoteId(note.getId());

            request.setAttribute("note", note);
            request.setAttribute("attachments", attachments);
            request.setAttribute("shareToken", token);
            request.getRequestDispatcher("/WEB-INF/jsp/note/share-view.jsp")
                    .forward(request, response);

        } catch (ServiceException e) {
            // NOT_FOUND covers both "unknown token" and "revoked" — same response
            // to prevent oracle attacks.
            log.info("Share link access failed. code={} ip={}", e.getCode(), ipAddress);
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Share link not found or has been revoked.");
        }
    }

    /**
     * Serves an attachment to a share-link visitor.
     *
     * <p>Authorization: the share token is resolved first to obtain the note ID;
     * the attachment must belong to that note. The requesting user's session
     * identity (if any) is NOT used for authorization here — the share token
     * is the sole credential.
     */
    private void handleShareAttachment(HttpServletRequest request, HttpServletResponse response,
                                       String token, Long attachmentId)
            throws ServletException, IOException {

        String ipAddress = request.getRemoteAddr();

        try {
            // Resolve the token to get the note ID (validates expiry / revocation).
            Note note = shareTokenService.resolveShareLink(token, ipAddress);

            // Serve the file using the share note ID as authorization.
            // requestingUserId is null (share-link access — no session required).
            fileService.serveFile(attachmentId, null, note.getId(), response);

        } catch (ServiceException e) {
            if ("NOT_FOUND".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else if ("ACCESS_DENIED".equals(e.getCode())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else {
                log.error("Unexpected error on share attachment. attachmentId={} ip={}",
                        attachmentId, ipAddress);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Long parseId(String segment) {
        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
