<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="fas fa-history text-info"></i> Activity Log</h2>
    <a href="${pageContext.request.contextPath}/admin" class="btn btn-secondary">
        <i class="fas fa-arrow-left"></i> Back to Dashboard
    </a>
</div>

<div class="card shadow">
    <div class="card-body">
        <c:if test="${empty recentActivity}">
            <p class="text-muted text-center">No activity logs found</p>
        </c:if>
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Time</th>
                        <th>User</th>
                        <th>Action</th>
                        <th>Entity</th>
                        <th>Details</th>
                        <th>IP Address</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="log" items="${recentActivity}">
                        <tr>
                            <td>${log.id}</td>
                            <td><small>${log.createdAt}</small></td>
                            <td><small>${log.user.username}</small></td>
                            <td><span class="badge badge-secondary">${log.action}</span></td>
                            <td><small>${log.entityType}</small></td>
                            <td><small>${log.details}</small></td>
                            <td><small>${log.ipAddress}</small></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
