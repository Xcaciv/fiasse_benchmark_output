<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:include page="layout.jsp">
    <jsp:param name="pageTitle" value="Top Rated"/>
</jsp:include>
<jsp:body>
<div class="row mb-4">
    <div class="col">
        <h2>Top Rated Notes</h2>
        <p class="text-muted">Public notes with at least 3 ratings, sorted by average rating</p>
    </div>
</div>

<c:if test="${empty topNotes}">
    <div class="text-center py-5">
        <i class="bi bi-trophy" style="font-size: 4rem; color: #6c757d;"></i>
        <h4 class="mt-3 text-muted">No top rated notes yet</h4>
        <p class="text-muted">Public notes need at least 3 ratings to appear here.</p>
        <a href="${pageContext.request.contextPath}/notes" class="btn btn-primary">Browse Notes</a>
    </div>
</c:if>

<div class="row">
    <c:forEach var="note" items="${topNotes}">
        <div class="col-md-6 col-lg-4 mb-4">
            <div class="card note-card h-100">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <h5 class="card-title mb-0">
                            <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" 
                               class="text-decoration-none text-dark">${note.title}</a>
                        </h5>
                    </div>
                    <p class="note-excerpt">${note.excerpt}</p>
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="text-muted small">By ${note.owner.username}</span>
                        <span class="star-rating">
                            <i class="bi bi-star-fill"></i> ${String.format("%.1f", note.averageRating)}
                        </span>
                    </div>
                </div>
                <div class="card-footer bg-white border-top-0">
                    <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" 
                       class="btn btn-outline-primary btn-sm w-100">View Note</a>
                </div>
            </div>
        </div>
    </c:forEach>
</div>
</jsp:body>
