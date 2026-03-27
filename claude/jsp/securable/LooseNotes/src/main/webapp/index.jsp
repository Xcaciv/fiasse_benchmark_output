<%-- Welcome / redirect page --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
    Object user = session.getAttribute("user");
    if (user != null) {
        response.sendRedirect(ctx + "/notes");
    } else {
        response.sendRedirect(ctx + "/auth/login");
    }
%>
