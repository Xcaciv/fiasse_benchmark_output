package com.loosenotes.web;

import com.loosenotes.dao.ActivityLogDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.security.SecurityUtil;
import com.loosenotes.util.AppUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "ProfileServlet", urlPatterns = {"/profile"})
public class ProfileServlet extends BaseServlet {
    private final UserDao userDao = new UserDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            if (requireLogin(request, response) == null) {
                return;
            }
            render(request, response, "profile/view.jsp", "Your profile");
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = requireLogin(request, response);
            if (user == null || !requireCsrf(request, response)) {
                return;
            }
            String username = AppUtil.trimToEmpty(request.getParameter("username"));
            String email = AppUtil.trimToEmpty(request.getParameter("email"));
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");

            List<String> errors = new ArrayList<>();
            AppUtil.validateUsername(username, errors);
            AppUtil.validateEmail(email, errors);
            if (userDao.usernameExists(username, user.getId())) {
                errors.add("That username is already in use.");
            }
            if (userDao.emailExists(email, user.getId())) {
                errors.add("That email address is already in use.");
            }
            boolean changingPassword = !(AppUtil.trimToEmpty(newPassword).isBlank() && AppUtil.trimToEmpty(confirmPassword).isBlank() && AppUtil.trimToEmpty(currentPassword).isBlank());
            if (changingPassword) {
                if (!SecurityUtil.verifyPassword(currentPassword == null ? "" : currentPassword, user.getPasswordSalt(), user.getPasswordHash())) {
                    errors.add("Current password is incorrect.");
                }
                AppUtil.validatePassword(newPassword, errors);
                if (!(newPassword == null ? "" : newPassword).equals(confirmPassword == null ? "" : confirmPassword)) {
                    errors.add("Password confirmation does not match.");
                }
            }
            if (!errors.isEmpty()) {
                request.setAttribute("errors", errors);
                render(request, response, "profile/view.jsp", "Your profile");
                return;
            }

            userDao.updateProfile(user.getId(), username, email);
            if (changingPassword) {
                SecurityUtil.PasswordHash hash = SecurityUtil.hashPassword(newPassword);
                userDao.updatePassword(user.getId(), hash.getHashBase64(), hash.getSaltBase64());
            }
            activityLogDao.log(user.getId(), "profile.update", "Profile updated.");
            setFlash(request, "success", "Profile updated successfully.");
            response.sendRedirect(request.getContextPath() + "/profile");
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
