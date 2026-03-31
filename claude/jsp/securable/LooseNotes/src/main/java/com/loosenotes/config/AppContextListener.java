package com.loosenotes.config;

import com.loosenotes.dao.*;
import com.loosenotes.service.*;
import com.loosenotes.util.RateLimiter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Initializes application-scoped services on startup and registers them in
 * the ServletContext for injection into servlets.
 *
 * SSEM notes:
 * - Modifiability: all configuration comes from app.properties; services wired once here.
 * - Testability: services stored as context attributes; tests can replace them.
 * - Availability: schema init happens at startup; failures prevent app deployment.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        log.info("LooseNotes starting up");

        Properties props = loadProperties();

        // ── Database ─────────────────────────────────────────────────────────
        DatabaseManager db = DatabaseManager.getInstance();
        db.initializeSchema();

        // ── DAOs ──────────────────────────────────────────────────────────────
        UserDao               userDao    = new UserDao(db);
        NoteDao               noteDao    = new NoteDao(db);
        AttachmentDao         attachDao  = new AttachmentDao(db);
        RatingDao             ratingDao  = new RatingDao(db);
        ShareLinkDao          shareDao   = new ShareLinkDao(db);
        AuditLogDao           auditDao   = new AuditLogDao(db);
        PasswordResetTokenDao prtDao     = new PasswordResetTokenDao(db);

        // ── Core services ────────────────────────────────────────────────────
        AuditService auditService = new AuditService(auditDao);

        int loginMax     = intProp(props, "rateLimit.login.maxAttempts", 5);
        int loginWindow  = intProp(props, "rateLimit.login.windowSeconds", 300);
        RateLimiter loginLimiter = new RateLimiter(loginMax, loginWindow);

        UserService userService = new UserService(userDao, auditService, loginLimiter);

        int maxTitle   = intProp(props, "app.note.maxTitleLength", 255);
        int maxContent = intProp(props, "app.note.maxContentLength", 65536);
        NoteService noteService = new NoteService(noteDao, auditService, maxTitle, maxContent);

        Path uploadDir = resolveUploadDir(props);
        long maxFileSize = longProp(props, "upload.maxFileSize", 5_242_880L);
        AttachmentService attachmentService =
                new AttachmentService(attachDao, noteDao, auditService, uploadDir, maxFileSize);

        RatingService ratingService = new RatingService(ratingDao, noteDao, auditService);

        ShareLinkService shareLinkService = new ShareLinkService(shareDao, noteDao, auditService);

        long tokenExpiry = longProp(props, "token.resetExpireSeconds", 3600L);
        PasswordResetService passwordResetService =
                new PasswordResetService(prtDao, userDao, auditService, tokenExpiry);

        // ── Register in context for servlet access ───────────────────────────
        ctx.setAttribute("userService",          userService);
        ctx.setAttribute("noteService",          noteService);
        ctx.setAttribute("attachmentService",    attachmentService);
        ctx.setAttribute("ratingService",        ratingService);
        ctx.setAttribute("shareLinkService",     shareLinkService);
        ctx.setAttribute("auditService",         auditService);
        ctx.setAttribute("passwordResetService", passwordResetService);
        ctx.setAttribute("appBaseUrl",           props.getProperty("app.baseUrl", ""));

        log.info("LooseNotes started successfully");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("LooseNotes shutting down");
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/app.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            log.warn("Could not load app.properties, using defaults", e);
        }
        return props;
    }

    private Path resolveUploadDir(Properties props) {
        String dir = props.getProperty("upload.dir", "./uploads");
        Path path = Paths.get(dir).toAbsolutePath();
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory: " + path, e);
        }
        return path;
    }

    private int intProp(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    private long longProp(Properties p, String key, long def) {
        try { return Long.parseLong(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
}
