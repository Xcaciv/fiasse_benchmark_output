package com.loosenotes.filter;

import com.loosenotes.util.SessionUtil;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String loginURI = httpRequest.getContextPath() + "/auth";
        String registerURI = httpRequest.getContextPath() + "/register";
        String resetURI = httpRequest.getContextPath() + "/auth?action=resetRequest";
        String resetTokenURI = httpRequest.getContextPath() + "/auth?action=resetToken";
        String indexURI = httpRequest.getContextPath() + "/";
        String shareURI = httpRequest.getContextPath() + "/share";

        boolean loggedIn = SessionUtil.isLoggedIn(session);
        boolean loginRequest = httpRequest.getRequestURI().equals(loginURI) ||
                               httpRequest.getRequestURI().equals(registerURI) ||
                               httpRequest.getRequestURI().equals(resetURI) ||
                               httpRequest.getRequestURI().equals(resetTokenURI) ||
                               httpRequest.getRequestURI().equals(indexURI) ||
                               httpRequest.getRequestURI().startsWith(shareURI) ||
                               httpRequest.getRequestURI().equals("/loose-notes/") ||
                               httpRequest.getRequestURI().equals("/loose-notes/index");

        if (loggedIn || loginRequest) {
            chain.doFilter(request, response);
        } else {
            httpResponse.sendRedirect(loginURI);
        }
    }

    @Override
    public void destroy() {
    }
}
