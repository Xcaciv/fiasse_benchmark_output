package com.loosenotes.bootstrap;

import com.loosenotes.config.AppConfig;
import com.loosenotes.context.AppContext;
import com.loosenotes.db.SchemaBootstrap;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.TimeUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Optional;

public class AppBootstrapListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AppConfig config = AppConfig.load();
        AppContext.initialize(config);
        SchemaBootstrap.initialize();

        AppContext appContext = AppContext.get();
        Optional<User> existingAdmin = appContext.getUserDao().findByUsername("admin");
        if (existingAdmin.isEmpty()) {
            User admin = appContext.getUserDao().create("admin", "admin@local.test", PasswordUtil.hash("Admin123!"), "ADMIN");
            appContext.getActivityLogDao().log(admin.getId(), "bootstrap.admin_seeded", "Seeded default administrator account at " + TimeUtil.format(admin.getCreatedAt()));
        }

        ServletContext servletContext = sce.getServletContext();
        servletContext.setAttribute("appName", "Loose Notes");
        servletContext.setAttribute("demoMode", config.isDemoMode());
    }
}
