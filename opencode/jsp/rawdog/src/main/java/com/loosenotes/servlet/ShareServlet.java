package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class ShareServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        
        if (token == null || token.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        shareLinkDAO.findByToken(token).ifPresentOrElse(
            shareLink -> {
                if (shareLink.isExpired()) {
                    try {
                        request.setAttribute("error", "This share link has expired");
                        request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                noteDAO.findById(shareLink.getNoteId()).ifPresentOrElse(
                    note -> {
                        if (!note.isShareEnabled()) {
                            try {
                                response.sendRedirect(request.getContextPath() + "/");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return;
                        }

                        List<Attachment> attachments = attachmentDAO.findByNoteId(note.getId());
                        List<Rating> ratings = ratingDAO.findByNoteId(note.getId());
                        
                        HttpSession session = request.getSession(false);
                        Rating userRating = null;
                        Long userId = SessionUtil.getUserId(session);
                        if (userId != null) {
                            userRating = ratingDAO.findByUserAndNote(userId, note.getId()).orElse(null);
                        }

                        request.setAttribute("note", note);
                        request.setAttribute("attachments", attachments);
                        request.setAttribute("ratings", ratings);
                        request.setAttribute("userRating", userRating);
                        request.setAttribute("isSharedView", true);
                        
                        try {
                            request.getRequestDispatcher("/WEB-INF/views/shared-note.jsp").forward(request, response);
                        } catch (ServletException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    () -> {
                        try {
                            response.sendRedirect(request.getContextPath() + "/");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                );
            },
            () -> {
                try {
                    request.setAttribute("error", "Invalid share link");
                    request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }
}
