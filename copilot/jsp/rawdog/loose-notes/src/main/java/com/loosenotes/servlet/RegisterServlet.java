package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        try {
            if (username == null || username.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.isEmpty()) {
                req.setAttribute("error", "All fields are required.");
                req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
                return;
            }
            if (!password.equals(confirmPassword)) {
                req.setAttribute("error", "Passwords do not match.");
                req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
                return;
            }
            if (userDAO.findByUsername(username) != null) {
                req.setAttribute("error", "Username already taken.");
                req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
                return;
            }
            if (userDAO.findByEmail(email) != null) {
                req.setAttribute("error", "Email already registered.");
                req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
                return;
            }
            String hash = PasswordUtil.hash(password);
            userDAO.createUser(username.trim(), email.trim(), hash);
            HttpSession session = req.getSession(true);
            session.setAttribute("successMessage", "Registration successful! Please log in.");
            resp.sendRedirect(req.getContextPath() + "/login");
        } catch (Exception e) {
            req.setAttribute("error", "Registration failed: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
        }
    }
}
