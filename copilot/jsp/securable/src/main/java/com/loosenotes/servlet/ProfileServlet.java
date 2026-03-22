package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.service.ServiceException;
import com.loosenotes.service.UserService;
import com.loosenotes.util.CsrfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Optional;

/**
 * User profile view and edit (REQ-014).
 * GET  /profile       → view profile
 * POST /profile/update→ update username/email
 * POST /profile/password → change password
 */
public final class ProfileServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ProfileServlet.class);

    private UserService userService;

    @Override
    public void init() {
        this.userService = (UserService) getServletContext().getAttribute("userService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long userId = (Long) req.getSession().getAttribute("userId");
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/logout");
            return;
        }
        req.setAttribute("user", userOpt.get());
        CsrfUtil.getOrCreateToken(req.getSession());
        req.getRequestDispatcher("/WEB-INF/jsp/user/profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        long userId = (Long) req.getSession().getAttribute("userId");

        if ("/update".equals(pathInfo)) {
            updateProfile(req, resp, userId);
        } else if ("/password".equals(pathInfo)) {
            changePassword(req, resp, userId);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void updateProfile(HttpServletRequest req, HttpServletResponse resp, long userId)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String email    = req.getParameter("email");
        try {
            userService.updateProfile(userId, username, email);
            // Update session username
            req.getSession().setAttribute("username", username);
            resp.sendRedirect(req.getContextPath() + "/profile?updated=1");
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("user", userService.findById(userId).orElse(null));
            req.getRequestDispatcher("/WEB-INF/jsp/user/profile.jsp").forward(req, resp);
        }
    }

    private void changePassword(HttpServletRequest req, HttpServletResponse resp, long userId)
            throws ServletException, IOException {
        String current = req.getParameter("currentPassword");
        String newPass  = req.getParameter("newPassword");
        String confirm  = req.getParameter("confirmPassword");

        if (newPass == null || !newPass.equals(confirm)) {
            req.setAttribute("pwError", "New passwords do not match");
            doGet(req, resp);
            return;
        }
        try {
            userService.changePassword(userId, current, newPass);
            resp.sendRedirect(req.getContextPath() + "/profile?pwchanged=1");
        } catch (ServiceException e) {
            req.setAttribute("pwError", e.getMessage());
            doGet(req, resp);
        }
    }
}
