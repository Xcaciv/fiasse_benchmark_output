package com.loosenotes.web;

import com.loosenotes.model.User;
import com.loosenotes.util.RandomTokenUtil;
import com.loosenotes.util.RequestUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@WebServlet("/auth/forgot-password")
public class ForgotPasswordServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("pageTitle", "Forgot Password");
        render(request, response, "auth/forgot-password");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireCsrf(request, response)) {
            return;
        }

        String email = ValidationUtil.normalizeEmail(request.getParameter("email"));
        request.setAttribute("pageTitle", "Forgot Password");
        request.setAttribute("emailValue", email);

        try {
            ValidationUtil.requireEmail(email);
            Optional<User> user = app().getUserDao().findByEmail(email);
            request.setAttribute("successMessage", "If an account exists for that email address, a reset link has been generated.");
            if (user.isPresent()) {
                String token = RandomTokenUtil.generate();
                app().getPasswordResetDao().createToken(user.get().getId(), token, LocalDateTime.now().plusHours(1).toString());
                app().getActivityLogDao().log(user.get().getId(), "auth.password_reset_requested", "Password reset requested for " + user.get().getEmail() + '.');
                if (app().getConfig().isDemoMode()) {
                    request.setAttribute("demoResetLink", RequestUtil.baseUrl(request) + "/auth/reset-password?token=" + token);
                }
            }
            render(request, response, "auth/forgot-password");
        } catch (IllegalArgumentException ex) {
            request.setAttribute("errorMessage", ex.getMessage());
            render(request, response, "auth/forgot-password");
        }
    }
}
