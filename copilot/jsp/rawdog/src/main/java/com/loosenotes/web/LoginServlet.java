package com.loosenotes.web;

import com.loosenotes.model.User;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

@WebServlet("/auth/login")
public class LoginServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("pageTitle", "Sign In");
        render(request, response, "auth/login");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireCsrf(request, response)) {
            return;
        }

        String identifier = request.getParameter("identifier") == null ? "" : request.getParameter("identifier").trim();
        String password = request.getParameter("password") == null ? "" : request.getParameter("password");
        request.setAttribute("pageTitle", "Sign In");
        request.setAttribute("identifierValue", identifier);

        Optional<User> user = app().getUserDao().findByUsernameOrEmail(identifier);
        if (user.isPresent() && PasswordUtil.matches(password, user.get().getPasswordHash())) {
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("authUser", user.get());
            CsrfUtil.token(newSession);
            app().getActivityLogDao().log(user.get().getId(), "auth.login", "Successful sign-in for " + user.get().getUsername() + '.');
            redirect(request, response, "/notes");
            return;
        }

        app().getActivityLogDao().log(null, "auth.login_failed", "Failed sign-in attempt for identifier " + identifier + '.');
        request.setAttribute("errorMessage", "Invalid username/email or password.");
        render(request, response, "auth/login");
    }
}
