package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            request.setAttribute("error", "Username is required.");
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
            return;
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            request.setAttribute("error", "A valid email is required.");
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
            return;
        }
        if (password == null || password.length() < 6) {
            request.setAttribute("error", "Password must be at least 6 characters.");
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
            return;
        }
        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "Passwords do not match.");
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
            return;
        }

        username = username.trim();
        email = email.trim().toLowerCase();

        try {
            // Check username uniqueness
            if (userDAO.findByUsername(username) != null) {
                request.setAttribute("error", "Username already taken.");
                request.setAttribute("username", username);
                request.setAttribute("email", email);
                request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
                return;
            }
            // Check email uniqueness
            if (userDAO.findByEmail(email) != null) {
                request.setAttribute("error", "Email already registered.");
                request.setAttribute("username", username);
                request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
                return;
            }

            String passwordHash = PasswordUtil.hashPassword(password);
            int newUserId = userDAO.create(username, email, passwordHash);

            if (newUserId < 0) {
                request.setAttribute("error", "Registration failed. Please try again.");
                request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
                return;
            }

            // Log in the new user
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", newUserId);
            session.setAttribute("username", username);
            session.setAttribute("userRole", "USER");

            activityLogDAO.log(newUserId, "REGISTER", "User registered: " + username);

            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (SQLException e) {
            getServletContext().log("Registration error", e);
            request.setAttribute("error", "A database error occurred. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
        }
    }
}
