<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Redirect authenticated users to /home, others to /login --%>
<%
    if (session != null && session.getAttribute("userId") != null) {
        response.sendRedirect(request.getContextPath() + "/home");
    } else {
        response.sendRedirect(request.getContextPath() + "/login");
    }
%>
