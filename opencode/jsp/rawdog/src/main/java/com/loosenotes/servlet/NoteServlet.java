package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.LoggerUtil;
import com.loosenotes.util.SessionUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class NoteServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        Long userId = SessionUtil.getUserId(session);

        if (action == null) {
            List<Note> notes = noteDAO.findByUserId(userId);
            request.setAttribute("notes", notes);
            request.getRequestDispatcher("/WEB-INF/views/notes.jsp").forward(request, response);
        } else if ("create".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/note-create.jsp").forward(request, response);
        } else if ("view".equals(action)) {
            viewNote(request, response);
        } else if ("edit".equals(action)) {
            editNoteView(request, response, userId);
        } else if ("delete".equals(action)) {
            deleteNote(request, response, userId);
        } else if ("myNotes".equals(action)) {
            List<Note> notes = noteDAO.findByUserId(userId);
            request.setAttribute("notes", notes);
            request.getRequestDispatcher("/WEB-INF/views/notes.jsp").forward(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/notes");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        Long userId = SessionUtil.getUserId(session);

        if ("create".equals(action)) {
            createNote(request, response, userId);
        } else if ("update".equals(action)) {
            updateNote(request, response, userId);
        } else if ("delete".equals(action)) {
            deleteNoteConfirm(request, response, userId);
        } else if ("share".equals(action)) {
            shareNote(request, response, userId);
        } else {
            response.sendRedirect(request.getContextPath() + "/notes");
        }
    }

    private void createNote(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws ServletException, IOException {
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String isPublicStr = request.getParameter("isPublic");
        boolean isPublic = "on".equals(isPublicStr);

        if (!ValidationUtil.isNotEmpty(title) || !ValidationUtil.isNotEmpty(content)) {
            request.setAttribute("error", "Title and content are required");
            request.getRequestDispatcher("/WEB-INF/views/note-create.jsp").forward(request, response);
            return;
        }

        Note note = new Note(userId, ValidationUtil.sanitize(title), ValidationUtil.sanitize(content));
        note.setPublic(isPublic);

        if (noteDAO.createNote(note)) {
            LoggerUtil.logNoteCreate(userId, note.getId(), title, request);
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + note.getId());
        } else {
            request.setAttribute("error", "Failed to create note");
            request.getRequestDispatcher("/WEB-INF/views/note-create.jsp").forward(request, response);
        }
    }

    private void viewNote(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long noteId = parseLong(request.getParameter("id"));
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }

        noteDAO.findById(noteId).ifPresentOrElse(
            note -> {
                HttpSession session = request.getSession(false);
                Long userId = SessionUtil.getUserId(session);
                boolean isOwner = userId != null && userId.equals(note.getUserId());
                
                if (!note.isPublic() && !isOwner && !SessionUtil.isAdmin(session)) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                List<Attachment> attachments = attachmentDAO.findByNoteId(noteId);
                List<Rating> ratings = ratingDAO.findByNoteId(noteId);
                
                Rating userRating = null;
                if (userId != null) {
                    userRating = ratingDAO.findByUserAndNote(userId, noteId).orElse(null);
                }

                request.setAttribute("note", note);
                request.setAttribute("attachments", attachments);
                request.setAttribute("ratings", ratings);
                request.setAttribute("userRating", userRating);
                request.setAttribute("isOwner", isOwner);
                
                try {
                    request.getRequestDispatcher("/WEB-INF/views/note-view.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/notes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void editNoteView(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws ServletException, IOException {
        Long noteId = parseLong(request.getParameter("id"));
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }

        noteDAO.findById(noteId).ifPresentOrElse(
            note -> {
                HttpSession session = request.getSession(false);
                boolean isOwner = userId.equals(note.getUserId());
                boolean isAdmin = SessionUtil.isAdmin(session);
                
                if (!isOwner && !isAdmin) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                List<Attachment> attachments = attachmentDAO.findByNoteId(noteId);
                request.setAttribute("note", note);
                request.setAttribute("attachments", attachments);
                
                try {
                    request.getRequestDispatcher("/WEB-INF/views/note-edit.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/notes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void updateNote(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws ServletException, IOException {
        Long noteId = parseLong(request.getParameter("id"));
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String isPublicStr = request.getParameter("isPublic");
        boolean isPublic = "on".equals(isPublicStr);

        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }

        noteDAO.findById(noteId).ifPresentOrElse(
            note -> {
                HttpSession session = request.getSession(false);
                boolean isOwner = userId.equals(note.getUserId());
                boolean isAdmin = SessionUtil.isAdmin(session);
                
                if (!isOwner && !isAdmin) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                if (!ValidationUtil.isNotEmpty(title) || !ValidationUtil.isNotEmpty(content)) {
                    request.setAttribute("error", "Title and content are required");
                    request.setAttribute("note", note);
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/note-edit.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                note.setTitle(ValidationUtil.sanitize(title));
                note.setContent(ValidationUtil.sanitize(content));
                note.setPublic(isPublic);

                if (noteDAO.updateNote(note)) {
                    LoggerUtil.logNoteUpdate(userId, noteId, title, request);
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    request.setAttribute("error", "Failed to update note");
                    request.setAttribute("note", note);
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/note-edit.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/notes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void deleteNote(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws ServletException, IOException {
        Long noteId = parseLong(request.getParameter("id"));
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }

        noteDAO.findById(noteId).ifPresentOrElse(
            note -> {
                boolean isOwner = userId.equals(note.getUserId());
                boolean isAdmin = SessionUtil.isAdmin(request.getSession(false));
                
                if (!isOwner && !isAdmin) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                request.setAttribute("note", note);
                try {
                    request.getRequestDispatcher("/WEB-INF/views/note-delete.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/notes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void deleteNoteConfirm(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws IOException {
        Long noteId = parseLong(request.getParameter("id"));
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }

        noteDAO.findById(noteId).ifPresentOrElse(
            note -> {
                boolean isOwner = userId.equals(note.getUserId());
                boolean isAdmin = SessionUtil.isAdmin(request.getSession(false));
                
                if (!isOwner && !isAdmin) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                String title = note.getTitle();
                noteDAO.deleteNote(noteId);
                LoggerUtil.logNoteDelete(userId, noteId, title, request);
                
                try {
                    response.sendRedirect(request.getContextPath() + "/notes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/notes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void shareNote(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws IOException {
        Long noteId = parseLong(request.getParameter("id"));
        String enableShare = request.getParameter("enableShare");

        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }

        noteDAO.findById(noteId).ifPresentOrElse(
            note -> {
                boolean isOwner = userId.equals(note.getUserId());
                
                if (!isOwner) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                if ("true".equals(enableShare)) {
                    String shareToken = UUID.randomUUID().toString().replace("-", "");
                    note.setShareToken(shareToken);
                    note.setShareEnabled(true);
                    
                    shareLinkDAO.findByNoteId(noteId).ifPresentOrElse(
                        link -> {
                            link.setShareToken(shareToken);
                            shareLinkDAO.updateToken(noteId, shareToken);
                        },
                        () -> {
                            ShareLink newLink = new ShareLink(noteId, shareToken);
                            shareLinkDAO.createShareLink(newLink);
                        }
                    );
                } else {
                    note.setShareEnabled(false);
                }

                noteDAO.updateNote(note);
                LoggerUtil.logNoteShare(userId, noteId, note.getTitle(), request);
                
                try {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/notes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
