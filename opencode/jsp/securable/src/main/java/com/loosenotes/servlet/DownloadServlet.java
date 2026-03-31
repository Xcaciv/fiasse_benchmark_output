package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.model.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet("/download")
public class DownloadServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DownloadServlet.class);
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String filename = request.getParameter("file");
        
        if (filename == null || filename.isEmpty() || filename.contains("..")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
            return;
        }
        
        try {
            Optional<Attachment> attachmentOpt = attachmentDAO.findByStoredFilename(filename);
            
            if (attachmentOpt.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }
            
            Attachment attachment = attachmentOpt.get();
            
            String uploadPath = getServletContext().getRealPath("/uploads");
            File file = new File(uploadPath, filename);
            
            if (!file.exists()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }
            
            response.setContentType(attachment.getContentType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + attachment.getOriginalFilename() + "\"");
            response.setContentLengthLong(file.length());
            
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Database error during file download", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }
}
