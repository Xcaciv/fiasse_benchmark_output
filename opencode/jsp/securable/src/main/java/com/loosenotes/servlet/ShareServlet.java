package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/share")
public class ShareServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ShareServlet.class);
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        
        if (token != null && !token.isEmpty()) {
            viewSharedNote(request, response, token);
        } else {
            handleShareManagement(request, response);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            if ("generate".equals(action)) {
                generateShareLink(request, response, user);
            } else if ("revoke".equals(action)) {
                revokeShareLink(request, response, user);
            } else if ("regenerate".equals(action)) {
                regenerateShareLink(request, response, user);
            } else {
                response.sendRedirect(request.getContextPath() + "/notes");
            }
        } catch (SQLException e) {
            logger.error("Database error in ShareServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }
    
    private void viewSharedNote(HttpServletRequest request, HttpServletResponse response, String token)
            throws SQLException, ServletException, IOException {
        ShareLink shareLink = shareLinkDAO.findByToken(token).orElse(null);
        
        if (shareLink == null || !shareLink.isValid()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Share link not found or expired");
            return;
        }
        
        Note note = noteDAO.findById(shareLink.getNoteId()).orElse(null);
        if (note == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Note not found");
            return;
        }
        
        note.setAttachments(attachmentDAO.findByNoteId(note.getId()));
        note.setRatings(ratingDAO.findByNoteId(note.getId()));
        note.setAverageRating(ratingDAO.getAverageRating(note.getId()));
        
        request.setAttribute("note", note);
        request.setAttribute("sharedNote", true);
        request.getRequestDispatcher("/WEB-INF/views/notes/view.jsp").forward(request, response);
    }
    
    private void handleShareManagement(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        Long noteId = getNoteIdFromRequest(request);
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        try {
            Note note = noteDAO.findById(noteId).orElse(null);
            if (note == null || (!note.getUserId().equals(user.getId()) && !user.isAdmin())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
            
            ShareLink activeLink = shareLinkDAO.findActiveByNoteId(noteId).orElse(null);
            
            request.setAttribute("note", note);
            request.setAttribute("shareLink", activeLink);
            request.getRequestDispatcher("/WEB-INF/views/notes/share.jsp").forward(request, response);
            
        } catch (SQLException e) {
            logger.error("Database error in handleShareManagement", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }
    
    private void generateShareLink(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        Long noteId = getNoteIdFromRequest(request);
        
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        Note note = noteDAO.findById(noteId).orElse(null);
        if (note == null || (!note.getUserId().equals(user.getId()) && !user.isAdmin())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        shareLinkDAO.deactivateByNoteId(noteId);
        
        ShareLink shareLink = new ShareLink(noteId);
        shareLinkDAO.create(shareLink);
        
        activityLogDAO.log(user.getId(), "SHARE_LINK_GENERATED", "Note ID: " + noteId, getClientIp(request));
        logger.info("Share link generated: noteId={}, userId={}", noteId, user.getId());
        
        response.sendRedirect(request.getContextPath() + "/share?id=" + noteId);
    }
    
    private void revokeShareLink(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        Long noteId = getNoteIdFromRequest(request);
        Long linkId = getLinkIdFromRequest(request);
        
        if (noteId == null || linkId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        Note note = noteDAO.findById(noteId).orElse(null);
        if (note == null || (!note.getUserId().equals(user.getId()) && !user.isAdmin())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        shareLinkDAO.deactivate(linkId);
        
        activityLogDAO.log(user.getId(), "SHARE_LINK_REVOKED", "Note ID: " + noteId, getClientIp(request));
        
        response.sendRedirect(request.getContextPath() + "/share?id=" + noteId);
    }
    
    private void regenerateShareLink(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        Long noteId = getNoteIdFromRequest(request);
        
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }
        
        Note note = noteDAO.findById(noteId).orElse(null);
        if (note == null || (!note.getUserId().equals(user.getId()) && !user.isAdmin())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        shareLinkDAO.deactivateByNoteId(noteId);
        
        ShareLink shareLink = new ShareLink(noteId);
        shareLinkDAO.create(shareLink);
        
        activityLogDAO.log(user.getId(), "SHARE_LINK_REGENERATED", "Note ID: " + noteId, getClientIp(request));
        
        response.sendRedirect(request.getContextPath() + "/share?id=" + noteId);
    }
    
    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }
    
    private Long getNoteIdFromRequest(HttpServletRequest request) {
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                return Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Long getLinkIdFromRequest(HttpServletRequest request) {
        String idParam = request.getParameter("linkId");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                return Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
