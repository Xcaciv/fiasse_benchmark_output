<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container-fluid mt-4">
    <div class="d-flex align-items-center mb-4">
        <h1 class="h3 mb-0 me-2">User Management</h1>
        <span class="badge bg-danger fs-6">ADMIN</span>
    </div>

    <%-- Search Form --%>
    <div class="card border-0 shadow-sm mb-4">
        <div class="card-body">
            <form method="get" action="${pageContext.request.contextPath}/admin/users"
                  class="row g-2 align-items-end">
                <div class="col-sm-8 col-md-6 col-lg-4">
                    <label for="searchQuery" class="form-label fw-semibold">Search Users</label>
                    <input type="text"
                           id="searchQuery"
                           name="q"
                           class="form-control"
                           placeholder="Username or email&hellip;"
                           value="<c:out value="${query}"/>">
                </div>
                <div class="col-auto">
                    <button type="submit" class="btn btn-primary">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                             class="bi bi-search me-1" viewBox="0 0 16 16">
                            <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.099zm-5.242 1.156a5.5 5.5 0 1 1 0-11 5.5 5.5 0 0 1 0 11z"/>
                        </svg>
                        Search
                    </button>
                    <c:if test="${not empty query}">
                        <a href="${pageContext.request.contextPath}/admin/users"
                           class="btn btn-outline-secondary ms-1">Clear</a>
                    </c:if>
                </div>
            </form>
        </div>
    </div>

    <%-- Results summary --%>
    <c:if test="${not empty query}">
        <p class="text-muted mb-3">
            Showing results for <strong><c:out value="${query}"/></strong>
            &mdash; <c:out value="${fn:length(users)}"/> user(s) found.
        </p>
    </c:if>

    <%-- Users Table --%>
    <div class="card border-0 shadow-sm">
        <div class="card-header bg-white border-bottom d-flex justify-content-between align-items-center">
            <h5 class="mb-0">Users</h5>
            <span class="badge bg-secondary rounded-pill">
                <c:out value="${fn:length(users)}"/> total
            </span>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover table-striped mb-0 align-middle">
                    <thead class="table-light">
                        <tr>
                            <th scope="col">#</th>
                            <th scope="col">Username</th>
                            <th scope="col">Email</th>
                            <th scope="col">Role</th>
                            <th scope="col">Status</th>
                            <th scope="col">Created</th>
                            <th scope="col" class="text-end">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty users}">
                                <tr>
                                    <td colspan="7" class="text-center text-muted py-4">
                                        No users found.
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="user" items="${users}">
                                    <tr>
                                        <td class="text-muted small">
                                            <c:out value="${user.id}"/>
                                        </td>
                                        <td class="fw-semibold">
                                            <c:out value="${user.username}"/>
                                        </td>
                                        <td>
                                            <c:out value="${user.email}"/>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${user.role eq 'ADMIN'}">
                                                    <span class="badge bg-danger">ADMIN</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-primary">USER</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${user.enabled}">
                                                    <span class="badge bg-success-subtle text-success border border-success-subtle">
                                                        Active
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">
                                                        Disabled
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-muted small text-nowrap">
                                            <fmt:formatDate value="${user.createdAt}"
                                                            pattern="yyyy-MM-dd"/>
                                        </td>
                                        <td class="text-end">
                                            <a href="${pageContext.request.contextPath}/admin/reassign/<c:out value="${user.id}"/>"
                                               class="btn btn-sm btn-outline-secondary">
                                                Manage
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
