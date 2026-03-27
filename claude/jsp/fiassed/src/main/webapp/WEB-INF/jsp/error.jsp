<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="error-page">
    <div class="card error-card">
        <h1 class="error-code">
            <c:choose>
                <c:when test="${not empty pageContext.errorData.statusCode}">
                    <c:out value="${pageContext.errorData.statusCode}"/>
                </c:when>
                <c:otherwise>Error</c:otherwise>
            </c:choose>
        </h1>

        <h2 class="error-title">
            <c:choose>
                <c:when test="${pageContext.errorData.statusCode == 400}">Bad Request</c:when>
                <c:when test="${pageContext.errorData.statusCode == 403}">Access Denied</c:when>
                <c:when test="${pageContext.errorData.statusCode == 404}">Page Not Found</c:when>
                <c:when test="${pageContext.errorData.statusCode == 405}">Method Not Allowed</c:when>
                <c:when test="${pageContext.errorData.statusCode == 500}">Internal Server Error</c:when>
                <c:otherwise>An Error Occurred</c:otherwise>
            </c:choose>
        </h2>

        <p class="error-message">
            <c:choose>
                <c:when test="${pageContext.errorData.statusCode == 403}">
                    You do not have permission to access this resource.
                </c:when>
                <c:when test="${pageContext.errorData.statusCode == 404}">
                    The page or resource you requested could not be found.
                </c:when>
                <c:when test="${pageContext.errorData.statusCode == 500}">
                    An unexpected error occurred. Please try again later.
                </c:when>
                <c:otherwise>
                    Something went wrong. Please try again or contact support if the problem persists.
                </c:otherwise>
            </c:choose>
        </p>

        <c:if test="${not empty correlationId}">
            <p class="error-correlation">
                Reference ID: <code><c:out value="${correlationId}"/></code>
            </p>
        </c:if>

        <div class="error-actions">
            <a href="${pageContext.request.contextPath}/notes" class="btn btn-primary">Go to Notes</a>
            <a href="javascript:history.back()" class="btn btn-secondary">Go Back</a>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
