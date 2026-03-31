package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ProfileServlet.class);
    private final UserDAO userDAO = new UserDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            if ("updateProfile".equals(action)) {
                updateProfile(request, response, user);
            } else if ("changePassword".equals(action)) {
                changePassword(request, response, user);
            } else {
                response.sendRedirect(request.getContextPath() + "/profile");
            }
        } catch (SQLException e) {
            logger.error("Database error in ProfileServlet", e);
            request.setAttribute("error", "An error occurred. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
        }
    }
    
    private void updateProfile(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException, ServletException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        
        if (username == null || username.trim().isEmpty()) {
            request.setAttribute("error", "Username is required");
            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
            return;
        }
        
        if (email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Email is required");
            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
            return;
        }
        
        try {
            User existingUser = userDAO.findByUsername(username.trim()).orElse(null);
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                request.setAttribute("error", "Username already exists");
                request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                return;
            }
            
            existingUser = userDAO.findByEmail(email.trim().toLowerCase()).orElse(null);
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                request.setAttribute("error", "Email already exists");
                request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                return;
            }
            
            user.setUsername(username.trim());
            user.setEmail(email.trim().toLowerCase());
            userDAO.update(user);
            
            HttpSession session = request.getSession();
            session.setAttribute("user", user);
            
            activityLogDAO.log(user.getId(), "PROFILE_UPDATED", "Profile updated", getClientIp(request));
            logger.info("Profile updated for user: {}", user.getUsername());
            
            response.sendRedirect(request.getContextPath() + "/profile?success=updated");
            
        } catch (SQLException e) {
            logger.error("Database error during profile update", e);
            request.setAttribute("error", "An error occurred. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
        }
    }
    
    private void changePassword(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException, ServletException {
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        
        if (currentPassword == null || currentPassword.isEmpty()) {
            request.setAttribute("error", "Current password is required");
            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
            return;
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            request.setAttribute("error", "New password must be at least 6 characters");
            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "New passwords do not match");
            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
            return;
        }
        
        try {
            User dbUser = userDAO.findById(user.getId()).orElse(null);
            if (dbUser == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            
            if (!BCrypt.checkpw(currentPassword, dbUser.getPasswordHash())) {
                request.setAttribute("error", "Current password is incorrect");
                request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                return;
            }
            
            String passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            userDAO.updatePassword(user.getId(), passwordHash);
            
            activityLogDAO.log(user.getId(), "PASSWORD_CHANGED", "Password changed", getClientIp(request));
            logger.info("Password changed for user: {}", user.getUsername());
            
            response.sendRedirect(request.getContextPath() + "/profile?success=passwordChanged");
            
        } catch (SQLException e) {
            logger.error("Database error during password change", e);
            request.setAttribute("error", "An error occurred. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
        }
    }
    
    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
