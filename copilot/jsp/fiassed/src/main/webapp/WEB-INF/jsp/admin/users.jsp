<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Manage Users - Loose Notes"/>
</jsp:include>
<div class="admin-page">
    <h2>Manage Users</h2>
    <p>Total users: ${totalUsers}</p>
    <table class="table">
        <thead>
            <tr><th>ID</th><th>Username</th><th>Email</th><th>Role</th><th>Created</th><th>Actions</th></tr>
        </thead>
        <tbody>
            <c:forEach var="user" items="${users}">
            <tr>
                <td>${user.id}</td>
                <td><c:out value="${user.username}"/></td>
                <td><c:out value="${user.email}"/></td>
                <td><c:out value="${user.role}"/></td>
                <td>${user.createdAt}</td>
                <td>
                    <c:if test="${user.id != sessionScope.userId}">
                    <form method="post" action="${pageContext.request.contextPath}/admin/users"
                          onsubmit="return confirmDelete()" style="display:inline">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="deleteUser">
                        <input type="hidden" name="userId" value="${user.id}">
                        <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                    </form>
                    </c:if>
                </td>
            </tr>
            </c:forEach>
        </tbody>
    </table>
    <div class="pagination">
        <c:if test="${currentPage > 1}">
            <a href="?page=${currentPage - 1}" class="btn btn-sm">&laquo; Prev</a>
        </c:if>
        <c:if test="${(currentPage * 20) < totalUsers}">
            <a href="?page=${currentPage + 1}" class="btn btn-sm">Next &raquo;</a>
        </c:if>
    </div>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
