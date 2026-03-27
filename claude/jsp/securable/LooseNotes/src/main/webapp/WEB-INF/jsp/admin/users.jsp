<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Users - Admin"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h1 class="h3 mb-4">User Management</h1>

<form method="get" action="${pageContext.request.contextPath}/admin/users" class="mb-4">
    <div class="input-group" style="max-width:400px">
        <input type="text" class="form-control" name="q" placeholder="Search username or email..."
               value="<c:out value='${searchQuery}'/>" maxlength="100">
        <button type="submit" class="btn btn-outline-secondary">Search</button>
    </div>
</form>

<c:if test="${error != null}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<div class="table-responsive">
    <table class="table table-hover">
        <thead class="table-dark">
            <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Email</th>
                <th>Role</th>
                <th>Registered</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="user" items="${users}">
                <tr>
                    <td><c:out value="${user.id}"/></td>
                    <td><c:out value="${user.username}"/></td>
                    <td><c:out value="${user.email}"/></td>
                    <td>
                        <span class="badge ${user.admin ? 'bg-warning text-dark' : 'bg-secondary'}">
                            <c:out value="${user.role}"/>
                        </span>
                    </td>
                    <td><fmt:formatDate value="${user.createdAt}" pattern="MMM d, yyyy"/></td>
                    <td>
                        <a href="${pageContext.request.contextPath}/admin/reassign?userId=<c:out value='${user.id}'/>"
                           class="btn btn-sm btn-outline-primary">View Notes</a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty users}">
                <tr><td colspan="6" class="text-muted text-center">No users found.</td></tr>
            </c:if>
        </tbody>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
