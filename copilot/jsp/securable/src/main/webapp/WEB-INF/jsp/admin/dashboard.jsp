<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Admin Dashboard"/>
</jsp:include>

<h1 class="h3 mb-4">⚙️ Admin Dashboard</h1>

<!-- Overview cards -->
<c:if test="${not empty userCount}">
<div class="row g-3 mb-4">
    <div class="col-md-4">
        <div class="card text-bg-primary">
            <div class="card-body">
                <h5 class="card-title">Total Users</h5>
                <p class="card-text fs-2">${userCount}</p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card text-bg-success">
            <div class="card-body">
                <h5 class="card-title">Total Notes</h5>
                <p class="card-text fs-2">${noteCount}</p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card text-bg-secondary">
            <div class="card-body">
                <h5 class="card-title">Recent Activity</h5>
                <a href="#activity" class="btn btn-light btn-sm mt-1">View Log</a>
            </div>
        </div>
    </div>
</div>
</c:if>

<c:if test="${param.reassigned == '1'}">
    <div class="alert alert-success">Note ownership reassigned.</div>
</c:if>

<!-- User Management -->
<h4>Users</h4>
<form method="get" action="${pageContext.request.contextPath}/admin/users" class="mb-3">
    <div class="input-group w-50">
        <input type="text" name="q" class="form-control" placeholder="Search by username or email"
               value="<c:out value='${query}'/>">
        <button class="btn btn-outline-secondary" type="submit">Search</button>
    </div>
</form>

<c:if test="${not empty users}">
<div class="table-responsive">
    <table class="table table-sm table-hover">
        <thead class="table-light">
        <tr>
            <th>ID</th><th>Username</th><th>Email</th><th>Role</th><th>Joined</th><th>Notes</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="u" items="${users}">
            <tr>
                <td>${u.id}</td>
                <td><c:out value="${u.username}"/></td>
                <td><c:out value="${u.email}"/></td>
                <td><span class="badge ${u.role == 'ADMIN' ? 'bg-danger' : 'bg-secondary'}">
                    <c:out value="${u.role}"/>
                </span></td>
                <td>${u.createdAt}</td>
                <td>—</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
</c:if>

<!-- Note Reassignment -->
<hr>
<h4>Reassign Note Ownership</h4>
<form method="post" action="${pageContext.request.contextPath}/admin/notes/reassign">
    <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
    <div class="row g-2 align-items-end">
        <div class="col-auto">
            <label for="noteId" class="form-label">Note ID</label>
            <input type="number" id="noteId" name="noteId" class="form-control" min="1" required>
        </div>
        <div class="col-auto">
            <label for="newOwnerId" class="form-label">New Owner ID</label>
            <input type="number" id="newOwnerId" name="newOwnerId" class="form-control" min="1" required>
        </div>
        <div class="col-auto">
            <button type="submit" class="btn btn-warning"
                    onclick="return confirm('Reassign note ownership?')">Reassign</button>
        </div>
    </div>
</form>

<!-- Activity Log -->
<c:if test="${not empty recentActivity}">
<hr id="activity">
<h4>Recent Activity Log</h4>
<div class="table-responsive">
    <table class="table table-sm table-striped">
        <thead class="table-dark">
        <tr>
            <th>Time</th><th>Actor</th><th>Action</th>
            <th>Resource</th><th>IP</th><th>Outcome</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="entry" items="${recentActivity}">
            <tr>
                <td class="small">${entry.createdAt}</td>
                <td><c:out value="${entry.actorUsername}"/></td>
                <td><c:out value="${entry.action}"/></td>
                <td><c:out value="${entry.resourceType}"/>/<c:out value="${entry.resourceId}"/></td>
                <td class="small"><c:out value="${entry.ipAddress}"/></td>
                <td><span class="badge ${entry.outcome == 'SUCCESS' ? 'bg-success' : 'bg-danger'}">
                    <c:out value="${entry.outcome}"/>
                </span></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
</c:if>

<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
