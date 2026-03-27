package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            request.setAttribute("errorMessage", "Please enter username and password.");
            request.setAttribute("inputUsername", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        try {
            User user = userDAO.findByUsername(username.trim());
            if (user == null || !BCrypt.checkpw(password, user.getPasswordHash())) {
                request.setAttribute("errorMessage", "Invalid username or password.");
                request.setAttribute("inputUsername", username);
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                return;
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Login failed: " + e.getMessage());
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }
}
