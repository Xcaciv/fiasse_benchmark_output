package com.loosenotes.web;

import com.loosenotes.model.User;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/auth/register")
public class RegisterServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("pageTitle", "Register");
        render(request, response, "auth/register");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireCsrf(request, response)) {
            return;
        }

        String username = ValidationUtil.normalizeUsername(request.getParameter("username"));
        String email = ValidationUtil.normalizeEmail(request.getParameter("email"));
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        request.setAttribute("pageTitle", "Register");
        request.setAttribute("usernameValue", username);
        request.setAttribute("emailValue", email);

        try {
            ValidationUtil.requireUsername(username);
            ValidationUtil.requireEmail(email);
            ValidationUtil.requirePassword(password);
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            if (app().getUserDao().findByUsername(username).isPresent()) {
                throw new IllegalArgumentException("That username is already in use.");
            }
            if (app().getUserDao().findByEmail(email).isPresent()) {
                throw new IllegalArgumentException("That email address is already in use.");
            }

            User user = app().getUserDao().create(username, email, com.loosenotes.util.PasswordUtil.hash(password), "USER");
            app().getActivityLogDao().log(user.getId(), "auth.register", "Registered account for " + username + '.');
            setFlash(request, "success", "Registration complete. You can now sign in.");
            redirect(request, response, "/auth/login");
        } catch (IllegalArgumentException ex) {
            request.setAttribute("errorMessage", ex.getMessage());
            render(request, response, "auth/register");
        }
    }
}
