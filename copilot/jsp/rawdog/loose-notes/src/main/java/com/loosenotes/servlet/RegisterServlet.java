package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String email    = request.getParameter("email");
        String password = request.getParameter("password");

        // Validate
        if (username == null || username.trim().length() < 3) {
            request.setAttribute("errorMessage", "Username must be at least 3 characters.");
            request.setAttribute("inputUsername", username);
            request.setAttribute("inputEmail", email);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            request.setAttribute("errorMessage", "Please provide a valid email address.");
            request.setAttribute("inputUsername", username);
            request.setAttribute("inputEmail", email);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }
        if (password == null || password.length() < 6) {
            request.setAttribute("errorMessage", "Password must be at least 6 characters.");
            request.setAttribute("inputUsername", username);
            request.setAttribute("inputEmail", email);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        try {
            if (userDAO.findByUsername(username.trim()) != null) {
                request.setAttribute("errorMessage", "Username already taken.");
                request.setAttribute("inputUsername", username);
                request.setAttribute("inputEmail", email);
                request.getRequestDispatcher("/register.jsp").forward(request, response);
                return;
            }
            if (userDAO.findByEmail(email.trim()) != null) {
                request.setAttribute("errorMessage", "Email already registered.");
                request.setAttribute("inputUsername", username);
                request.setAttribute("inputEmail", email);
                request.getRequestDispatcher("/register.jsp").forward(request, response);
                return;
            }

            User user = new User();
            user.setUsername(username.trim());
            user.setEmail(email.trim());
            user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            user.setRole("USER");
            userDAO.create(user);

            request.getSession().setAttribute("successMessage", "Registration successful! Please log in.");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Registration failed: " + e.getMessage());
            request.setAttribute("inputUsername", username);
            request.setAttribute("inputEmail", email);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }
}
