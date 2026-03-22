package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.util.TokenUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@WebServlet("/notes/create")
@MultipartConfig(maxFileSize = 10485760)
public class NoteCreateServlet extends HttpServlet {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"));

    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/notes/create.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = (Integer) req.getSession().getAttribute("userId");
        String title = req.getParameter("title");
        String content = req.getParameter("content");
        boolean isPublic = "true".equals(req.getParameter("isPublic"));

        try {
            if (title == null || title.trim().isEmpty()) {
                req.setAttribute("error", "Title is required.");
                req.getRequestDispatcher("/WEB-INF/views/notes/create.jsp").forward(req, resp);
                return;
            }
            int noteId = noteDAO.createNote(userId, title.trim(), content != null ? content : "", isPublic);

            Part filePart = req.getPart("file");
            if (filePart != null && filePart.getSize() > 0) {
                String originalFilename = getFileName(filePart);
                if (originalFilename != null && !originalFilename.isEmpty()) {
                    String ext = getExtension(originalFilename);
                    if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
                        req.setAttribute("error", "File type not allowed.");
                        req.getRequestDispatcher("/WEB-INF/views/notes/create.jsp").forward(req, resp);
                        return;
                    }
                    String storedFilename = TokenUtil.generateToken() + "." + ext;
                    String uploadDir = getServletContext().getRealPath("/") + "uploads";
                    new File(uploadDir).mkdirs();
                    filePart.write(uploadDir + File.separator + storedFilename);
                    attachmentDAO.addAttachment(noteId, originalFilename, storedFilename);
                }
            }
            activityLogDAO.log(userId, "NOTE_CREATE", "Created note id=" + noteId + " title=" + title);
            resp.sendRedirect(req.getContextPath() + "/notes");
        } catch (Exception e) {
            req.setAttribute("error", "Error creating note: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/notes/create.jsp").forward(req, resp);
        }
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        if (contentDisp == null) return null;
        for (String s : contentDisp.split(";")) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot + 1) : "";
    }
}
