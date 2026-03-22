<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Admin Dashboard – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<h2 class="mb-4">Admin Dashboard</h2>

<c:if test="${param.msg == 'reassigned'}">
  <div class="alert alert-success">Note ownership reassigned successfully.</div>
</c:if>

<%-- Stats row --%>
<div class="row mb-4">
  <div class="col-md-3">
    <div class="card text-center shadow-sm">
      <div class="card-body">
        <h3><c:out value="${totalUsers}"/></h3>
        <p class="text-muted mb-0">Total Users</p>
      </div>
    </div>
  </div>
  <div class="col-md-3">
    <div class="card text-center shadow-sm">
      <div class="card-body">
        <h3><c:out value="${totalNotes}"/></h3>
        <p class="text-muted mb-0">Total Notes</p>
      </div>
    </div>
  </div>
</div>

<%-- Recent Notes --%>
<h5 class="mb-2">Recent Notes</h5>
<div class="table-responsive mb-4">
  <table class="table table-sm table-bordered">
    <thead class="table-dark">
      <tr>
        <th>ID</th><th>Title</th><th>Author</th><th>Visibility</th><th>Created</th><th>Action</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="n" items="${recentNotes}">
        <tr>
          <td>${n.id}</td>
          <td>
            <a href="${pageContext.request.contextPath}/notes/view?id=${n.id}">
              <c:out value="${n.title}"/>
            </a>
          </td>
          <td><c:out value="${n.authorUsername}"/></td>
          <td>${n.public ? 'Public' : 'Private'}</td>
          <td><c:out value="${n.createdAt}"/></td>
          <td>
            <button class="btn btn-xs btn-outline-secondary btn-sm"
                    data-bs-toggle="modal"
                    data-bs-target="#reassignModal${n.id}">Reassign</button>
            <%-- Reassign Modal --%>
            <div class="modal fade" id="reassignModal${n.id}" tabindex="-1">
              <div class="modal-dialog">
                <div class="modal-content">
                  <div class="modal-header">
                    <h5 class="modal-title">Reassign Note #${n.id}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                  </div>
                  <form method="post"
                        action="${pageContext.request.contextPath}/admin/reassign">
                    <div class="modal-body">
                      <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                      <input type="hidden" name="noteId"    value="${n.id}">
                      <label class="form-label">New Owner ID</label>
                      <input type="number" name="newOwnerId" class="form-control"
                             required min="1" placeholder="User ID">
                    </div>
                    <div class="modal-footer">
                      <button type="button" class="btn btn-secondary"
                              data-bs-dismiss="modal">Cancel</button>
                      <button type="submit" class="btn btn-warning">Reassign</button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div>

<%-- User Management --%>
<div class="d-flex justify-content-between align-items-center mb-2">
  <h5>Users</h5>
  <form method="get" action="${pageContext.request.contextPath}/admin/dashboard"
        class="d-flex gap-2">
    <input type="text" name="userSearch" class="form-control form-control-sm"
           placeholder="Search by username or email"
           value="<c:out value='${userSearch}'/>" maxlength="100">
    <button type="submit" class="btn btn-sm btn-secondary">Search</button>
  </form>
</div>

<div class="table-responsive">
  <table class="table table-sm table-bordered table-hover">
    <thead class="table-dark">
      <tr><th>ID</th><th>Username</th><th>Email</th><th>Role</th><th>Registered</th></tr>
    </thead>
    <tbody>
      <c:forEach var="u" items="${users}">
        <tr>
          <td>${u.id}</td>
          <td><c:out value="${u.username}"/></td>
          <td><c:out value="${u.email}"/></td>
          <td><c:out value="${u.role}"/></td>
          <td><c:out value="${u.createdAt}"/></td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
