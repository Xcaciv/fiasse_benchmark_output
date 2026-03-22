package com.loosenotes.context;

import com.loosenotes.config.AppConfig;
import com.loosenotes.dao.ActivityLogDao;
import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.PasswordResetDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.dao.UserDao;

public final class AppContext {
    private static AppContext instance;

    private final AppConfig config;
    private final UserDao userDao;
    private final NoteDao noteDao;
    private final AttachmentDao attachmentDao;
    private final RatingDao ratingDao;
    private final ShareLinkDao shareLinkDao;
    private final PasswordResetDao passwordResetDao;
    private final ActivityLogDao activityLogDao;

    private AppContext(AppConfig config) {
        this.config = config;
        this.userDao = new UserDao();
        this.noteDao = new NoteDao();
        this.attachmentDao = new AttachmentDao();
        this.ratingDao = new RatingDao();
        this.shareLinkDao = new ShareLinkDao();
        this.passwordResetDao = new PasswordResetDao();
        this.activityLogDao = new ActivityLogDao();
    }

    public static synchronized void initialize(AppConfig config) {
        if (instance == null) {
            instance = new AppContext(config);
        }
    }

    public static AppContext get() {
        if (instance == null) {
            throw new IllegalStateException("Application context has not been initialized.");
        }
        return instance;
    }

    public AppConfig getConfig() {
        return config;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public NoteDao getNoteDao() {
        return noteDao;
    }

    public AttachmentDao getAttachmentDao() {
        return attachmentDao;
    }

    public RatingDao getRatingDao() {
        return ratingDao;
    }

    public ShareLinkDao getShareLinkDao() {
        return shareLinkDao;
    }

    public PasswordResetDao getPasswordResetDao() {
        return passwordResetDao;
    }

    public ActivityLogDao getActivityLogDao() {
        return activityLogDao;
    }
}
