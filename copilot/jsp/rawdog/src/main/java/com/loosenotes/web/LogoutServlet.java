package com.loosenotes.web;

import com.loosenotes.model.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/auth/logout")
public class LogoutServlet extends BaseServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireCsrf(request, response)) {
            return;
        }

        User user = currentUser(request);
        if (user != null) {
            app().getActivityLogDao().log(user.getId(), "auth.logout", "Signed out " + user.getUsername() + '.');
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirect(request, response, "/");
    }
}
