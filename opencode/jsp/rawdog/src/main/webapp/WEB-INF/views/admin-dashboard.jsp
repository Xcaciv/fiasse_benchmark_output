<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<h2 class="mb-4"><i class="fas fa-shield-alt text-danger"></i> Admin Dashboard</h2>

<div class="row mb-4">
    <div class="col-md-4">
        <div class="card bg-primary text-white">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h6 class="text-uppercase">Total Users</h6>
                        <h2 class="mb-0">${totalUsers}</h2>
                    </div>
                    <i class="fas fa-users fa-3x opacity-50"></i>
                </div>
            </div>
            <div class="card-footer bg-transparent border-0">
                <a href="${pageContext.request.contextPath}/admin?action=users" class="text-white">View All <i class="fas fa-arrow-right"></i></a>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card bg-success text-white">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h6 class="text-uppercase">Total Notes</h6>
                        <h2 class="mb-0">${totalNotes}</h2>
                    </div>
                    <i class="fas fa-file-alt fa-3x opacity-50"></i>
                </div>
            </div>
            <div class="card-footer bg-transparent border-0">
                <a href="${pageContext.request.contextPath}/admin?action=notes" class="text-white">View All <i class="fas fa-arrow-right"></i></a>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card bg-info text-white">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h6 class="text-uppercase">Recent Activity</h6>
                        <h2 class="mb-0">${recentActivity.size()}</h2>
                    </div>
                    <i class="fas fa-history fa-3x opacity-50"></i>
                </div>
            </div>
            <div class="card-footer bg-transparent border-0">
                <a href="${pageContext.request.contextPath}/admin?action=activity" class="text-white">View All <i class="fas fa-arrow-right"></i></a>
            </div>
        </div>
    </div>
</div>

<h4 class="mb-3"><i class="fas fa-history"></i> Recent Activity</h4>
<div class="card shadow">
    <div class="card-body">
        <c:if test="${empty recentActivity}">
            <p class="text-muted text-center">No recent activity</p>
        </c:if>
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>Time</th>
                        <th>User</th>
                        <th>Action</th>
                        <th>Details</th>
                        <th>IP Address</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="log" items="${recentActivity}">
                        <tr>
                            <td><small>${log.createdAt}</small></td>
                            <td><small>${log.user.username}</small></td>
                            <td><span class="badge badge-secondary">${log.action}</span></td>
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
