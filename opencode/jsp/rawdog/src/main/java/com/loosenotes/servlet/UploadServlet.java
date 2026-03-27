package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.util.LoggerUtil;
import com.loosenotes.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UploadServlet extends HttpServlet {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
        "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"
    ));
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    
    private String uploadPath;

    @Override
    public void init() throws ServletException {
        uploadPath = getServletContext().getRealPath("/uploads");
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        
        if (!SessionUtil.isLoggedIn(request.getSession(false))) {
            response.sendRedirect(request.getContextPath() + "/auth");
            return;
        }

        Long userId = SessionUtil.getUserId(request.getSession(false));
        Long noteId = parseLong(request.getParameter("noteId"));
        String action = request.getParameter("action");

        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/notes");
            return;
        }

        if ("delete".equals(action)) {
            deleteAttachment(request, response, userId, noteId);
        } else {
            uploadAttachment(request, response, userId, noteId);
        }
    }

    private void uploadAttachment(HttpServletRequest request, HttpServletResponse response, Long userId, Long noteId)
            throws ServletException, IOException {
        noteDAO.findById(noteId).ifPresentOrElse(
            note -> {
                boolean isOwner = userId.equals(note.getUserId());
                boolean isAdmin = SessionUtil.isAdmin(request.getSession(false));
                
                if (!isOwner && !isAdmin) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                try {
                    Part filePart = request.getPart("file");
                    if (filePart == null || filePart.getSize() == 0) {
                        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                        return;
                    }

                    String originalFilename = getFileName(filePart);
                    String extension = getFileExtension(originalFilename).toLowerCase();
                    
                    if (!ALLOWED_EXTENSIONS.contains(extension)) {
                        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&error=invalidFileType");
                        return;
                    }

                    if (filePart.getSize() > MAX_FILE_SIZE) {
                        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&error=fileTooLarge");
                        return;
                    }

                    String storedFilename = UUID.randomUUID().toString() + "." + extension;
                    File uploadedFile = new File(uploadPath, storedFilename);
                    
                    try (InputStream input = filePart.getInputStream()) {
                        Files.copy(input, uploadedFile.toPath());
                    }

                    Attachment attachment = new Attachment(
                        noteId,
                        originalFilename,
                        storedFilename,
                        "/uploads/" + storedFilename,
                        filePart.getSize(),
                        filePart.getContentType()
                    );

                    if (attachmentDAO.createAttachment(attachment)) {
                        LoggerUtil.logFileUpload(userId, noteId, originalFilename, request);
                    }

                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                } catch (Exception e) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&error=uploadFailed");
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
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

    private void deleteAttachment(HttpServletRequest request, HttpServletResponse response, Long userId, Long noteId)
            throws IOException {
        Long attachmentId = parseLong(request.getParameter("attachmentId"));
        
        if (attachmentId == null) {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            return;
        }

        attachmentDAO.findById(attachmentId).ifPresentOrElse(
            attachment -> {
                noteDAO.findById(noteId).ifPresentOrElse(
                    note -> {
                        boolean isOwner = userId.equals(note.getUserId());
                        boolean isAdmin = SessionUtil.isAdmin(request.getSession(false));
                        
                        if (!isOwner && !isAdmin) {
                            try {
                                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return;
                        }

                        Path filePath = Paths.get(uploadPath, attachment.getStoredFilename());
                        try {
                            Files.deleteIfExists(filePath);
                        } catch (IOException e) {
                        }

                        attachmentDAO.deleteAttachment(attachmentId);
                        
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
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String item : items) {
            if (item.trim().startsWith("filename")) {
                return item.substring(item.indexOf("=") + 2, item.length() - 1);
            }
        }
        return "";
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "";
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
