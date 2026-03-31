package com.loosenotes.filter;

import com.loosenotes.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WebFilter("/*")
public class AuthenticationFilter implements Filter {
    
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/",
        "/login",
        "/register",
        "/logout",
        "/top-rated",
        "/share",
        "/css/",
        "/js/",
        "/uploads/"
    );
    
    private static final List<String> SERVLET_PUBLIC_PATHS = Arrays.asList(
        "LoginServlet",
        "RegisterServlet", 
        "LogoutServlet",
        "TopRatedServlet",
        "ShareServlet"
    );
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = requestURI.substring(contextPath.length());
        
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/uploads/")) {
            chain.doFilter(request, response);
            return;
        }
        
        if (path.equals("/") || path.equals("") || path.equals("/index.jsp")) {
            httpRequest.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);
            return;
        }
        
        boolean isPublicPath = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        
        if (isPublicPath) {
            if (path.equals("/login") || path.equals("/register") || path.equals("/logout")) {
                HttpSession session = httpRequest.getSession(false);
                User user = session != null ? (User) session.getAttribute("user") : null;
                
                if (user != null && (path.equals("/login") || path.equals("/register"))) {
                    httpResponse.sendRedirect(contextPath + "/notes");
                    return;
                }
            }
            chain.doFilter(request, response);
            return;
        }
        
        HttpSession session = httpRequest.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        
        if (user == null) {
            session = httpRequest.getSession(true);
            session.setAttribute("redirectAfterLogin", requestURI);
            httpResponse.sendRedirect(contextPath + "/login");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
    }
}
