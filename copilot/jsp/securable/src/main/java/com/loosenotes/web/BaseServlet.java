package com.loosenotes.web;

import com.loosenotes.dao.UserDao;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.security.SecurityUtil;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class BaseServlet extends HttpServlet {
    protected final UserDao userDao = new UserDao();

    protected User currentUser(HttpServletRequest request) throws SQLException {
        Object cached = request.getAttribute("currentUser");
        if (cached instanceof User) {
            return (User) cached;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute("userId");
        if (!(userId instanceof Long)) {
            return null;
        }
        User user = userDao.findById((Long) userId).orElse(null);
        if (user != null) {
            request.setAttribute("currentUser", user);
        }
        return user;
    }

    protected User requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        User user = currentUser(request);
        if (user == null) {
            setFlash(request, "error", "Please log in to continue.");
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return null;
        }
        return user;
    }

    protected User requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        User user = requireLogin(request, response);
        if (user == null) {
            return null;
        }
        if (!user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access is required.");
            return null;
        }
        return user;
    }

    protected boolean requireCsrf(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!SecurityUtil.isValidCsrf(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid CSRF token.");
            return false;
        }
        return true;
    }

    protected void render(HttpServletRequest request, HttpServletResponse response, String jspPath, String pageTitle)
            throws ServletException, IOException, SQLException {
        prepareLayout(request, pageTitle);
        request.getRequestDispatcher("/WEB-INF/jsp/" + jspPath).forward(request, response);
    }

    protected void prepareLayout(HttpServletRequest request, String pageTitle) throws SQLException {
        request.setAttribute("pageTitle", pageTitle);
        User user = currentUser(request);
        request.setAttribute("isAuthenticated", user != null);
        request.setAttribute("isAdmin", user != null && user.isAdmin());
        request.setAttribute("currentUser", user);
        HttpSession session = request.getSession(false);
        if (session == null && shouldCreateSessionForView(request, user)) {
            session = request.getSession();
        }
        request.setAttribute("csrfToken", session == null ? "" : SecurityUtil.csrfToken(session));
        request.setAttribute("flashSuccess", session == null ? null : popFlash(session, "flashSuccess"));
        request.setAttribute("flashError", session == null ? null : popFlash(session, "flashError"));
        request.setAttribute("flashShareUrl", session == null ? null : popFlash(session, "flashShareUrl"));
    }

    protected void setFlash(HttpServletRequest request, String kind, String message) {
        HttpSession session = request.getSession();
        String key = "success".equalsIgnoreCase(kind) ? "flashSuccess" : "flashError";
        session.setAttribute(key, message);
    }

    protected void setFlashValue(HttpServletRequest request, String key, String value) {
        request.getSession().setAttribute(key, value);
    }

    protected boolean canView(Note note, User user) {
        return note != null && (note.isPublicNote() || (user != null && (user.getId() == note.getOwnerId() || user.isAdmin())));
    }

    protected boolean canManage(Note note, User user) {
        return note != null && user != null && (user.getId() == note.getOwnerId() || user.isAdmin());
    }

    protected boolean isOwner(Note note, User user) {
        return note != null && user != null && user.getId() == note.getOwnerId();
    }

    protected String appUrl(HttpServletRequest request, String relativePath) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme()).append("://").append(request.getServerName());
        if (!((request.isSecure() && request.getServerPort() == 443) || (!request.isSecure() && request.getServerPort() == 80))) {
            builder.append(':').append(request.getServerPort());
        }
        builder.append(request.getContextPath()).append(relativePath);
        return builder.toString();
    }

    private Object popFlash(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        if (value != null) {
            session.removeAttribute(key);
        }
        return value;
    }

    private boolean shouldCreateSessionForView(HttpServletRequest request, User user) {
        if (user != null) {
            return true;
        }
        String servletPath = request.getServletPath();
        return servletPath.startsWith("/auth");
    }
}
