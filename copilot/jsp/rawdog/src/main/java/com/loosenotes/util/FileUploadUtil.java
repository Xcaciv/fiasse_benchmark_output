package com.loosenotes.util;

import com.loosenotes.config.AppConfig;
import com.loosenotes.context.AppContext;
import com.loosenotes.model.Attachment;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class FileUploadUtil {
    private FileUploadUtil() {
    }

    public static List<Attachment> saveAttachments(long noteId, Collection<Part> parts) throws IOException {
        AppConfig config = AppContext.get().getConfig();
        List<Part> fileParts = new ArrayList<>();
        for (Part part : parts) {
            if (!"attachments".equals(part.getName()) || part.getSize() == 0) {
                continue;
            }
            String submittedFileName = part.getSubmittedFileName();
            if (submittedFileName == null || submittedFileName.isBlank()) {
                continue;
            }
            String safeFileName = Path.of(submittedFileName).getFileName().toString();
            if (!ValidationUtil.isAllowedExtension(safeFileName)) {
                throw new IllegalArgumentException("Unsupported attachment type for file: " + safeFileName);
            }
            if (part.getSize() > config.getMaxUploadBytes()) {
                throw new IllegalArgumentException("File exceeds the upload limit: " + safeFileName);
            }
            fileParts.add(part);
        }

        List<Attachment> attachments = new ArrayList<>();
        for (Part part : fileParts) {
            String safeFileName = Path.of(part.getSubmittedFileName()).getFileName().toString();
            String storageName = RandomTokenUtil.generate();
            Path destination = config.getUploadDirectory().resolve(storageName);
            try (InputStream inputStream = part.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            Attachment attachment = AppContext.get().getAttachmentDao().create(
                    noteId,
                    storageName,
                    safeFileName,
                    part.getContentType(),
                    part.getSize());
            attachments.add(attachment);
        }
        return attachments;
    }

    public static void deleteStoredFiles(List<Attachment> attachments) throws IOException {
        Path uploadDirectory = AppContext.get().getConfig().getUploadDirectory();
        for (Attachment attachment : attachments) {
            Files.deleteIfExists(uploadDirectory.resolve(attachment.getStorageName()));
        }
    }
}
