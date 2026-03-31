package com.loosenotes.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles the application root (/). Redirects logged-in users to their notes;
 * unauthenticated users see the landing page.
 */
@WebServlet("/")
public class HomeServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (getCurrentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/notes");
        } else {
            forward(req, resp, "home.jsp");
        }
    }
}
