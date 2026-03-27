package com.loosenotes.util;

import com.loosenotes.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SessionUtil {

    public static final String USER_SESSION_KEY = "loggedInUser";
    public static final String USER_ID_KEY = "userId";
    public static final String USER_ROLE_KEY = "userRole";

    public static void setUser(HttpSession session, User user) {
        session.setAttribute(USER_SESSION_KEY, user);
        session.setAttribute(USER_ID_KEY, user.getId());
        session.setAttribute(USER_ROLE_KEY, user.getRole());
    }

    public static User getUser(HttpSession session) {
        return (User) session.getAttribute(USER_SESSION_KEY);
    }

    public static Long getUserId(HttpSession session) {
        Object userId = session.getAttribute(USER_ID_KEY);
        if (userId instanceof Long) {
            return (Long) userId;
        } else if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return null;
    }

    public static String getUserRole(HttpSession session) {
        return (String) session.getAttribute(USER_ROLE_KEY);
    }

    public static boolean isLoggedIn(HttpSession session) {
        return getUser(session) != null;
    }

    public static boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(getUserRole(session));
    }

    public static void removeUser(HttpSession session) {
        session.removeAttribute(USER_SESSION_KEY);
        session.removeAttribute(USER_ID_KEY);
        session.removeAttribute(USER_ROLE_KEY);
    }

    public static void invalidate(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public static String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
