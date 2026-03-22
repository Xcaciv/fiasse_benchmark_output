package com.loosenotes.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Objects;

public final class CsrfUtil {
    public static final String SESSION_KEY = "csrfToken";

    private CsrfUtil() {
    }

    public static String token(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_KEY);
        if (token == null) {
            token = RandomTokenUtil.generate();
            session.setAttribute(SESSION_KEY, token);
        }
        return token;
    }

    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        return Objects.equals(request.getParameter("csrfToken"), session.getAttribute(SESSION_KEY));
    }
}
