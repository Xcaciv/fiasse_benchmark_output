package com.loosenotes.web;

import com.loosenotes.context.AppContext;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.model.User;
import com.loosenotes.util.CsrfUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

public abstract class BaseServlet extends HttpServlet {
    protected AppContext app() {
        return AppContext.get();
    }

    protected void render(HttpServletRequest request, HttpServletResponse response, String viewPath) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.getRequestDispatcher("/WEB-INF/views/" + viewPath + ".jsp").forward(request, response);
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    protected User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("authUser");
    }

    protected boolean requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (currentUser(request) == null) {
            setFlash(request, "error", "Please sign in to continue.");
            redirect(request, response, "/auth/login");
            return false;
        }
        return true;
    }

    protected boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User currentUser = currentUser(request);
        if (currentUser == null || !currentUser.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access is required.");
            return false;
        }
        return true;
    }

    protected boolean requireCsrf(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!CsrfUtil.isValid(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid CSRF token.");
            return false;
        }
        return true;
    }

    protected Long requireLongParameter(HttpServletRequest request, HttpServletResponse response, String name) throws IOException {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter: " + name + '.');
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter: " + name + '.');
            return null;
        }
    }

    protected Integer requireIntParameter(HttpServletRequest request, HttpServletResponse response, String name) throws IOException {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter: " + name + '.');
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter: " + name + '.');
            return null;
        }
    }

    protected void setFlash(HttpServletRequest request, String type, String message) {
        request.getSession(true).setAttribute("flash" + Character.toUpperCase(type.charAt(0)) + type.substring(1), message);
    }

    protected boolean canManage(Note note, User user) {
        return user != null && (note.getUserId() == user.getId() || user.isAdmin());
    }

    protected boolean canView(Note note, User user, String shareToken) {
        if (note.isPublic()) {
            return true;
        }
        if (user != null && note.getUserId() == user.getId()) {
            return true;
        }
        if (shareToken == null || shareToken.isBlank()) {
            return false;
        }
        Optional<ShareLink> shareLink = app().getShareLinkDao().findByToken(shareToken);
        return shareLink.isPresent() && shareLink.get().getNoteId() == note.getId();
    }
}
