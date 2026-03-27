package com.loosenotes.servlet;

import com.loosenotes.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser != null) {
            resp.sendRedirect(req.getContextPath() + "/notes");
        } else {
            req.getRequestDispatcher("/WEB-INF/views/home/index.jsp").forward(req, resp);
        }
    }
}
