<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>Manage Users</h1>
    <a href="${pageContext.request.contextPath}/admin" class="btn btn-secondary">Back to Dashboard</a>
</div>

<form method="get" action="${pageContext.request.contextPath}/admin/users" class="search-form">
    <div class="search-bar">
        <input type="text"
               id="q"
               name="q"
               placeholder="Search by username or email..."
               maxlength="255"
               value="${fn:escapeXml(searchQuery)}"/>
        <button type="submit" class="btn btn-primary">Search</button>
        <c:if test="${not empty searchQuery}">
            <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-secondary">Clear</a>
        </c:if>
    </div>
</form>

<c:choose>
    <c:when test="${not empty users}">
        <table class="data-table users-table">
            <thead>
                <tr>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Created</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="user" items="${users}">
                    <tr>
                        <td><c:out value="${user.username}"/></td>
                        <td><c:out value="${user.email}"/></td>
                        <td><c:out value="${user.role}"/></td>
                        <td><c:out value="${user.createdAt}"/></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/admin/users/${user.id}/notes"
                               class="btn btn-small">View Notes</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <div class="pagination">
            <c:if test="${page > 1}">
                <a href="${pageContext.request.contextPath}/admin/users?page=${page - 1}&q=${fn:escapeXml(searchQuery)}"
                   class="btn btn-small">&laquo; Previous</a>
            </c:if>
            <span class="page-indicator">Page <c:out value="${page}"/></span>
            <c:if test="${fn:length(users) >= 20}">
                <a href="${pageContext.request.contextPath}/admin/users?page=${page + 1}&q=${fn:escapeXml(searchQuery)}"
                   class="btn btn-small">Next &raquo;</a>
            </c:if>
        </div>
    </c:when>
    <c:otherwise>
        <p class="empty-state">No users found.</p>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
