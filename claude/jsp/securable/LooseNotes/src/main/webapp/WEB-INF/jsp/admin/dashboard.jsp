<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container-fluid mt-4">
    <div class="d-flex align-items-center mb-4">
        <h1 class="h3 mb-0 me-2">Dashboard</h1>
        <span class="badge bg-danger fs-6">ADMIN</span>
    </div>

    <%-- Stats Cards --%>
    <div class="row g-4 mb-5">
        <div class="col-sm-6 col-xl-3">
            <div class="card border-0 shadow-sm h-100">
                <div class="card-body d-flex align-items-center gap-3">
                    <div class="rounded-circle bg-primary bg-opacity-10 p-3">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor"
                             class="bi bi-people-fill text-primary" viewBox="0 0 16 16">
                            <path d="M7 14s-1 0-1-1 1-4 5-4 5 3 5 4-1 1-1 1H7zm4-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"/>
                            <path fill-rule="evenodd"
                                  d="M5.216 14A2.238 2.238 0 0 1 5 13c0-1.355.68-2.75 1.936-3.72A6.325 6.325 0 0 0 5 9c-4 0-5 3-5 4s1 1 1 1h4.216z"/>
                            <path d="M4.5 8a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z"/>
                        </svg>
                    </div>
                    <div>
                        <div class="text-muted small">Total Users</div>
                        <div class="fs-4 fw-semibold"><c:out value="${userCount}"/></div>
                    </div>
                </div>
                <div class="card-footer bg-transparent border-0 pt-0">
                    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-sm btn-outline-primary w-100">
                        View Users
                    </a>
                </div>
            </div>
        </div>

        <div class="col-sm-6 col-xl-3">
            <div class="card border-0 shadow-sm h-100">
                <div class="card-body d-flex align-items-center gap-3">
                    <div class="rounded-circle bg-success bg-opacity-10 p-3">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor"
                             class="bi bi-journal-text text-success" viewBox="0 0 16 16">
                            <path d="M5 10.5a.5.5 0 0 1 .5-.5h2a.5.5 0 0 1 0 1h-2a.5.5 0 0 1-.5-.5zm0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5zm0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5z"/>
                            <path d="M3 0h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2v-1h1v1a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1v1H1V2a2 2 0 0 1 2-2z"/>
                            <path d="M1 5v-.5a.5.5 0 0 1 1 0V5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1H1zm0 3v-.5a.5.5 0 0 1 1 0V8h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1H1zm0 3v-.5a.5.5 0 0 1 1 0v.5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1H1z"/>
                        </svg>
                    </div>
                    <div>
                        <div class="text-muted small">Total Notes</div>
                        <div class="fs-4 fw-semibold"><c:out value="${noteCount}"/></div>
                    </div>
                </div>
                <div class="card-footer bg-transparent border-0 pt-0">
                    <a href="${pageContext.request.contextPath}/notes" class="btn btn-sm btn-outline-success w-100">
                        View Notes
                    </a>
                </div>
            </div>
        </div>
    </div>

    <%-- Quick Actions --%>
    <div class="row mb-4">
        <div class="col-12">
            <div class="d-flex gap-2 flex-wrap">
                <a href="${pageContext.request.contextPath}/admin/users"
                   class="btn btn-outline-secondary">
                    <i class="bi bi-people me-1"></i> Manage Users
                </a>
                <a href="${pageContext.request.contextPath}/admin/reassign"
                   class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left-right me-1"></i> Reassign Note
                </a>
            </div>
        </div>
    </div>

    <%-- Recent Activity --%>
    <div class="card border-0 shadow-sm">
        <div class="card-header bg-white border-bottom">
            <h5 class="mb-0">Recent Activity</h5>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover table-striped mb-0 align-middle">
                    <thead class="table-light">
                        <tr>
                            <th scope="col">Timestamp</th>
                            <th scope="col">Event Type</th>
                            <th scope="col">User</th>
                            <th scope="col">Detail</th>
                            <th scope="col">IP Address</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty activity}">
                                <tr>
                                    <td colspan="5" class="text-center text-muted py-4">
                                        No activity recorded yet.
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="entry" items="${activity}">
                                    <tr>
                                        <td class="text-nowrap text-muted small">
                                            <fmt:formatDate value="${entry.createdAt}"
                                                            pattern="yyyy-MM-dd HH:mm:ss"/>
                                        </td>
                                        <td>
                                            <%-- Choose badge colour by event type prefix --%>
                                            <c:choose>
                                                <c:when test="${fn:containsIgnoreCase(entry.eventType, 'login') or fn:containsIgnoreCase(entry.eventType, 'auth')}">
                                                    <span class="badge bg-info text-dark">
                                                        <c:out value="${entry.eventType}"/>
                                                    </span>
                                                </c:when>
                                                <c:when test="${fn:containsIgnoreCase(entry.eventType, 'fail') or fn:containsIgnoreCase(entry.eventType, 'error') or fn:containsIgnoreCase(entry.eventType, 'denied')}">
                                                    <span class="badge bg-danger">
                                                        <c:out value="${entry.eventType}"/>
                                                    </span>
                                                </c:when>
                                                <c:when test="${fn:containsIgnoreCase(entry.eventType, 'delete') or fn:containsIgnoreCase(entry.eventType, 'remove')}">
                                                    <span class="badge bg-warning text-dark">
                                                        <c:out value="${entry.eventType}"/>
                                                    </span>
                                                </c:when>
                                                <c:when test="${fn:containsIgnoreCase(entry.eventType, 'create') or fn:containsIgnoreCase(entry.eventType, 'register')}">
                                                    <span class="badge bg-success">
                                                        <c:out value="${entry.eventType}"/>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary">
                                                        <c:out value="${entry.eventType}"/>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${empty entry.userId}">
                                                    <span class="text-muted fst-italic">Anonymous</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:out value="${entry.userId}"/>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-truncate" style="max-width: 300px;">
                                            <c:out value="${entry.eventDetail}"/>
                                        </td>
                                        <td class="text-muted small text-nowrap">
                                            <c:out value="${entry.ipAddress}"/>
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
