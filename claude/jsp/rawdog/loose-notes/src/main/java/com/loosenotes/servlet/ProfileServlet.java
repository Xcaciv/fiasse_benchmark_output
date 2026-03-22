package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");

        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/logout");
                return;
            }

            List<Note> notes = noteDAO.findByUserId(userId);
            int noteCount = notes.size();
            long publicNoteCount = notes.stream().filter(Note::isPublic).count();

            request.setAttribute("user", user);
            request.setAttribute("noteCount", noteCount);
            request.setAttribute("publicNoteCount", publicNoteCount);

            String success = request.getParameter("success");
            if (success != null) {
                request.setAttribute("success", success);
            }
            String error = request.getParameter("error");
            if (error != null) {
                request.setAttribute("error", error);
            }

            request.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Profile load error", e);
            request.setAttribute("errorMessage", "Failed to load profile.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");
        String action = request.getParameter("action");

        if ("updateEmail".equals(action)) {
            handleUpdateEmail(request, response, userId, session);
        } else if ("changePassword".equals(action)) {
            handleChangePassword(request, response, userId);
        } else {
            response.sendRedirect(request.getContextPath() + "/profile");
        }
    }

    private void handleUpdateEmail(HttpServletRequest request, HttpServletResponse response,
                                    int userId, HttpSession session) throws IOException, ServletException {
        String email = request.getParameter("email");
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            response.sendRedirect(request.getContextPath() + "/profile?error=Invalid+email+address");
            return;
        }

        email = email.trim().toLowerCase();

        try {
            User existing = userDAO.findByEmail(email);
            if (existing != null && existing.getId() != userId) {
                response.sendRedirect(request.getContextPath() + "/profile?error=Email+already+in+use");
                return;
            }
            userDAO.updateProfile(userId, email);
            activityLogDAO.log(userId, "UPDATE_PROFILE", "Updated email");
            response.sendRedirect(request.getContextPath() + "/profile?success=Email+updated+successfully");
        } catch (SQLException e) {
            getServletContext().log("Profile update error", e);
            response.sendRedirect(request.getContextPath() + "/profile?error=Update+failed");
        }
    }

    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response,
                                       int userId) throws IOException, ServletException {
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (currentPassword == null || newPassword == null || confirmPassword == null) {
            response.sendRedirect(request.getContextPath() + "/profile?error=All+password+fields+are+required");
            return;
        }

        if (newPassword.length() < 6) {
            response.sendRedirect(request.getContextPath() + "/profile?error=New+password+must+be+at+least+6+characters");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            response.sendRedirect(request.getContextPath() + "/profile?error=New+passwords+do+not+match");
            return;
        }

        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/logout");
                return;
            }

            if (!PasswordUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
                response.sendRedirect(request.getContextPath() + "/profile?error=Current+password+is+incorrect");
                return;
            }

            String newHash = PasswordUtil.hashPassword(newPassword);
            userDAO.updatePassword(userId, newHash);
            activityLogDAO.log(userId, "CHANGE_PASSWORD", "User changed password");
            response.sendRedirect(request.getContextPath() + "/profile?success=Password+changed+successfully");
        } catch (SQLException e) {
            getServletContext().log("Change password error", e);
            response.sendRedirect(request.getContextPath() + "/profile?error=Password+change+failed");
        }
    }
}
