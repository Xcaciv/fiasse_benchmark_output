package com.loosenotes.servlet;

import com.loosenotes.audit.AuditLogger;
import com.loosenotes.model.User;
import com.loosenotes.service.UserService;
import com.loosenotes.util.CsrfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Optional;

/** Handles user login and session creation (REQ-002). */
public final class LoginServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LoginServlet.class);

    private UserService userService;
    private AuditLogger auditLogger;

    @Override
    public void init() {
        this.userService  = (UserService)  getServletContext().getAttribute("userService");
        this.auditLogger  = (AuditLogger)  getServletContext().getAttribute("auditLogger");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // If already authenticated, redirect to notes list
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            resp.sendRedirect(req.getContextPath() + "/notes");
            return;
        }
        CsrfUtil.getOrCreateToken(req.getSession(true));
        req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        Optional<User> userOpt = userService.authenticate(username, password);
        if (userOpt.isEmpty()) {
            req.setAttribute("error", "Invalid username or password");
            CsrfUtil.getOrCreateToken(req.getSession(true));
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
            return;
        }

        User user = userOpt.get();
        // Invalidate old session to prevent session fixation (Authenticity)
        HttpSession oldSession = req.getSession(false);
        if (oldSession != null) oldSession.invalidate();

        HttpSession newSession = req.getSession(true);
        newSession.setAttribute("userId",       user.getId());
        newSession.setAttribute("username",     user.getUsername());
        newSession.setAttribute("userRole",     user.getRole());
        CsrfUtil.generateToken(newSession);

        auditLogger.log(user.getId(), user.getUsername(), "LOGIN_SUCCESS", "USER",
                String.valueOf(user.getId()), req.getRemoteAddr(), "SUCCESS", null);

        String redirect = req.getParameter("redirect");
        if (redirect != null && redirect.startsWith("/") && !redirect.contains("//")) {
            resp.sendRedirect(req.getContextPath() + redirect);
        } else {
            resp.sendRedirect(req.getContextPath() + "/notes");
        }
    }
}
