<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="text-center mb-4">
    <h2><i class="fas fa-star text-warning"></i> Top Rated Notes</h2>
    <p class="text-muted">Notes with at least ${minRatings} ratings, sorted by average rating</p>
</div>

<c:if test="${empty topRatedNotes}">
    <div class="alert alert-info text-center">
        <i class="fas fa-star-half-alt fa-2x mb-3"></i>
        <h4>No top rated notes yet</h4>
        <p>Notes need at least ${minRatings} ratings to appear here.</p>
        <a href="${pageContext.request.contextPath}/search" class="btn btn-primary">
            <i class="fas fa-search"></i> Browse Notes
        </a>
    </div>
</c:if>

<c:if test="${not empty topRatedNotes}">
    <div class="row">
        <c:forEach var="note" items="${topRatedNotes}" varStatus="status">
            <div class="col-md-6 mb-4">
                <div class="card h-100 shadow-sm border-warning">
                    <div class="card-header bg-warning text-dark">
                        <div class="d-flex justify-content-between align-items-center">
                            <span class="badge badge-dark">#${status.index + 1}</span>
                            <span class="h4 mb-0 text-warning">
                                <i class="fas fa-star"></i> ${String.format("%.1f", note.averageRating)}
                            </span>
                        </div>
                    </div>
                    <div class="card-body">
                        <h5 class="card-title">${note.title}</h5>
                        <p class="card-text text-muted">${note.excerpt}</p>
                        <div class="d-flex justify-content-between align-items-center">
                            <small class="text-muted">
                                <i class="fas fa-user"></i> ${note.owner.username}
                            </small>
                            <small class="text-muted">
                                <i class="fas fa-star-half-alt"></i> ${note.ratingCount} ratings
                            </small>
                        </div>
                    </div>
                    <div class="card-footer bg-white">
                        <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" class="btn btn-sm btn-outline-warning btn-block">
                            <i class="fas fa-eye"></i> View Note
                        </a>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
</c:if>

<jsp:include page="footer.jsp"/>
