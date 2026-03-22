package com.loosenotes.context;

import com.loosenotes.audit.AuditLogger;
import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.dao.impl.AuditLogDaoImpl;
import com.loosenotes.dao.impl.AttachmentDaoImpl;
import com.loosenotes.dao.impl.NoteDaoImpl;
import com.loosenotes.dao.impl.RatingDaoImpl;
import com.loosenotes.dao.impl.ShareLinkDaoImpl;
import com.loosenotes.dao.impl.UserDaoImpl;
import com.loosenotes.service.FileService;
import com.loosenotes.service.NoteService;
import com.loosenotes.service.UserService;
import com.loosenotes.service.impl.FileServiceImpl;
import com.loosenotes.service.impl.NoteServiceImpl;
import com.loosenotes.service.impl.UserServiceImpl;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

/**
 * Application bootstrap and teardown (Modifiability: DI wiring in one place).
 * All service instances are stored in ServletContext for servlet injection.
 */
public final class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    private DatabaseManager dbManager;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        String dbPath = resolveParam(ctx, "db.path",
                System.getProperty("user.home") + "/.loosenotes/loosenotes.db");
        ensureParentDirExists(dbPath);

        dbManager = new DatabaseManager(dbPath);

        // --- DAO wiring ---
        AuditLogDao  auditLogDao  = new AuditLogDaoImpl(dbManager);
        UserDao      userDao      = new UserDaoImpl(dbManager);
        NoteDao      noteDao      = new NoteDaoImpl(dbManager);
        AttachmentDao attachDao   = new AttachmentDaoImpl(dbManager);
        RatingDao    ratingDao    = new RatingDaoImpl(dbManager);
        ShareLinkDao shareLinkDao = new ShareLinkDaoImpl(dbManager);

        // --- Audit logger ---
        AuditLogger auditLogger = new AuditLogger(auditLogDao);

        // --- Service wiring ---
        String uploadDir = resolveParam(ctx, "upload.dir",
                System.getProperty("user.home") + "/.loosenotes/uploads");
        long maxUploadBytes = Long.parseLong(
                resolveParam(ctx, "upload.maxSizeBytes", "10485760"));
        ensureParentDirExists(uploadDir + "/placeholder");

        FileService fileService = new FileServiceImpl(uploadDir, maxUploadBytes);
        UserService userService = new UserServiceImpl(userDao, auditLogger);
        NoteService noteService = new NoteServiceImpl(
                noteDao, attachDao, ratingDao, shareLinkDao, fileService, auditLogger);

        // --- Publish to ServletContext ---
        ctx.setAttribute("userService",  userService);
        ctx.setAttribute("noteService",  noteService);
        ctx.setAttribute("fileService",  fileService);
        ctx.setAttribute("auditLogger",  auditLogger);
        ctx.setAttribute("auditLogDao",  auditLogDao);

        log.info("Application context initialized");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (dbManager != null) {
            dbManager.close();
        }
        log.info("Application context destroyed");
    }

    private String resolveParam(ServletContext ctx, String name, String defaultVal) {
        String val = ctx.getInitParameter(name);
        if (val == null || val.trim().isEmpty()) return defaultVal;
        // Resolve ${user.home} placeholder
        return val.replace("${user.home}", System.getProperty("user.home"));
    }

    private void ensureParentDirExists(String path) {
        File f = new File(path).getParentFile();
        if (f != null && !f.exists()) {
            f.mkdirs();
        }
    }
}
