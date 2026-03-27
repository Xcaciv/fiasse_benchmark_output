package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        // Validate
        if (username == null || username.isBlank()) {
            req.setAttribute("error", "Username is required.");
            forward(req, resp, username, email);
            return;
        }
        if (email == null || email.isBlank()) {
            req.setAttribute("error", "Email is required.");
            forward(req, resp, username, email);
            return;
        }
        if (password == null || password.length() < 6) {
            req.setAttribute("error", "Password must be at least 6 characters.");
            forward(req, resp, username, email);
            return;
        }
        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match.");
            forward(req, resp, username, email);
            return;
        }
        if (userDAO.findByUsername(username.trim()) != null) {
            req.setAttribute("error", "Username is already taken.");
            forward(req, resp, username, email);
            return;
        }
        if (userDAO.findByEmail(email.trim()) != null) {
            req.setAttribute("error", "Email is already registered.");
            forward(req, resp, username, email);
            return;
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole("USER");

        if (userDAO.create(user)) {
            req.getSession(true).setAttribute("flash_success", "Account created! Please log in.");
            resp.sendRedirect(req.getContextPath() + "/login");
        } else {
            req.setAttribute("error", "Registration failed. Please try again.");
            forward(req, resp, username, email);
        }
    }

    private void forward(HttpServletRequest req, HttpServletResponse resp,
                         String username, String email) throws ServletException, IOException {
        req.setAttribute("username", username);
        req.setAttribute("email", email);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
    }
}
