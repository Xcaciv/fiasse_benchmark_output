<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="fas fa-home"></i> Dashboard</h2>
    <a href="${pageContext.request.contextPath}/notes?action=create" class="btn btn-primary">
        <i class="fas fa-plus"></i> Create Note
    </a>
</div>

<c:if test="${not empty recentNotes}">
<div class="mb-5">
    <h4 class="mb-3"><i class="fas fa-globe text-success"></i> Recent Public Notes</h4>
    <div class="row">
        <c:forEach var="note" items="${recentNotes}" begin="0" end="2">
            <div class="col-md-4 mb-3">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h6 class="card-title">${note.title}</h6>
                        <p class="card-text text-muted small">${note.excerpt}</p>
                        <small class="text-muted">
                            <i class="fas fa-user"></i> ${note.owner.username}
                            <c:if test="${note.ratingCount > 0}">
                                <span class="text-warning ml-2"><i class="fas fa-star"></i> ${String.format("%.1f", note.averageRating)}</span>
                            </c:if>
                        </small>
                    </div>
                    <div class="card-footer bg-white">
                        <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" class="btn btn-sm btn-outline-primary btn-block">
                            View Note
                        </a>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
    <div class="text-right">
        <a href="${pageContext.request.contextPath}/search" class="btn btn-outline-success btn-sm">
            Browse More <i class="fas fa-arrow-right"></i>
        </a>
    </div>
</div>
</c:if>

<div class="row">
    <div class="col-md-4 mb-4">
        <div class="card border-left-primary shadow h-100 py-2">
            <div class="card-body">
                <div class="row no-gutters align-items-center">
                    <div class="col mr-2">
                        <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">Quick Actions</div>
                        <div class="mt-3">
                            <a href="${pageContext.request.contextPath}/notes?action=create" class="btn btn-primary btn-block mb-2">
                                <i class="fas fa-plus"></i> Create Note
                            </a>
                            <a href="${pageContext.request.contextPath}/notes?action=myNotes" class="btn btn-outline-primary btn-block mb-2">
                                <i class="fas fa-list"></i> My Notes
                            </a>
                            <a href="${pageContext.request.contextPath}/search" class="btn btn-outline-success btn-block">
                                <i class="fas fa-search"></i> Search Notes
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-md-4 mb-4">
        <div class="card border-left-success shadow h-100 py-2">
            <div class="card-body">
                <div class="row no-gutters align-items-center">
                    <div class="col mr-2">
                        <div class="text-xs font-weight-bold text-success text-uppercase mb-1">Discover</div>
                        <div class="mt-3">
                            <a href="${pageContext.request.contextPath}/top-rated" class="btn btn-warning btn-block mb-2">
                                <i class="fas fa-star"></i> Top Rated Notes
                            </a>
                            <a href="${pageContext.request.contextPath}/search" class="btn btn-outline-warning btn-block">
                                <i class="fas fa-search"></i> Search Notes
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-md-4 mb-4">
        <div class="card border-left-info shadow h-100 py-2">
            <div class="card-body">
                <div class="row no-gutters align-items-center">
                    <div class="col mr-2">
                        <div class="text-xs font-weight-bold text-info text-uppercase mb-1">Account</div>
                        <div class="mt-3">
                            <a href="${pageContext.request.contextPath}/profile" class="btn btn-info btn-block mb-2">
                                <i class="fas fa-user-cog"></i> Edit Profile
                            </a>
                            <c:if test="${sessionScope.userRole == 'ADMIN'}">
                                <a href="${pageContext.request.contextPath}/admin" class="btn btn-danger btn-block">
                                    <i class="fas fa-shield-alt"></i> Admin Panel
                                </a>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
