package com.loosenotes.web;

import com.loosenotes.model.PasswordResetToken;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@WebServlet("/auth/reset-password")
public class ResetPasswordServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = request.getParameter("token");
        request.setAttribute("pageTitle", "Reset Password");
        request.setAttribute("token", token);
        request.setAttribute("validToken", token != null && app().getPasswordResetDao().findUsableByToken(token).isPresent());
        render(request, response, "auth/reset-password");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireCsrf(request, response)) {
            return;
        }

        String token = request.getParameter("token");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        request.setAttribute("pageTitle", "Reset Password");
        request.setAttribute("token", token);

        Optional<PasswordResetToken> resetToken = token == null ? Optional.empty() : app().getPasswordResetDao().findUsableByToken(token);
        if (resetToken.isEmpty()) {
            request.setAttribute("validToken", false);
            request.setAttribute("errorMessage", "This reset link is invalid, expired, or already used.");
            render(request, response, "auth/reset-password");
            return;
        }

        try {
            ValidationUtil.requirePassword(password);
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            app().getUserDao().updatePassword(resetToken.get().getUserId(), PasswordUtil.hash(password));
            app().getPasswordResetDao().markUsed(token);
            app().getActivityLogDao().log(resetToken.get().getUserId(), "auth.password_reset_completed", "Completed password reset via emailed link.");
            setFlash(request, "success", "Password updated. Please sign in with your new password.");
            redirect(request, response, "/auth/login");
        } catch (IllegalArgumentException ex) {
            request.setAttribute("validToken", true);
            request.setAttribute("errorMessage", ex.getMessage());
            render(request, response, "auth/reset-password");
        }
    }
}
