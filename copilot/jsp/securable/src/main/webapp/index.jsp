<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
    <c:when test="${not empty sessionScope.userId}">
        <%
            response.sendRedirect(request.getContextPath() + "/notes");
        %>
    </c:when>
    <c:otherwise>
        <%
            response.sendRedirect(request.getContextPath() + "/login");
        %>
    </c:otherwise>
</c:choose>
