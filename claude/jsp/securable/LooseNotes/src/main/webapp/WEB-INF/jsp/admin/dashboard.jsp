<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Admin Dashboard - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h1 class="h3 mb-4">Admin Dashboard</h1>

<c:if test="${error != null}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<div class="row g-4 mb-4">
    <div class="col-md-3">
        <div class="card text-white bg-primary">
            <div class="card-body">
                <h5 class="card-title">Total Users</h5>
                <p class="card-text display-6"><c:out value="${userCount}"/></p>
                <a href="${pageContext.request.contextPath}/admin/users"
                   class="btn btn-light btn-sm">View All</a>
            </div>
        </div>
    </div>
</div>

<h4 class="h5 mb-3">Recent Activity</h4>

<c:choose>
    <c:when test="${empty recentActivity}">
        <p class="text-muted">No recent activity.</p>
    </c:when>
    <c:otherwise>
        <div class="table-responsive">
            <table class="table table-sm table-hover">
                <thead class="table-dark">
                    <tr>
                        <th>Time</th>
                        <th>Action</th>
                        <th>User</th>
                        <th>Resource</th>
                        <th>IP</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="log" items="${recentActivity}">
                        <tr>
                            <td class="text-nowrap">
                                <small><fmt:formatDate value="${log.createdAt}" pattern="MM/dd HH:mm"/></small>
                            </td>
                            <td><c:out value="${log.action}"/></td>
                            <td><c:out value="${log.userId != null ? log.userId : 'anonymous'}"/></td>
                            <td>
                                <c:if test="${log.resourceType != null}">
                                    <c:out value="${log.resourceType}"/>:<c:out value="${log.resourceId}"/>
                                </c:if>
                            </td>
                            <td><c:out value="${log.ipAddress}"/></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
