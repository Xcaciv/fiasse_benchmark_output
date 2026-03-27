<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="fas fa-file-alt text-success"></i> All Notes</h2>
    <a href="${pageContext.request.contextPath}/admin" class="btn btn-secondary">
        <i class="fas fa-arrow-left"></i> Back to Dashboard
    </a>
</div>

<div class="card shadow">
    <div class="card-body">
        <c:if test="${empty notes}">
            <p class="text-muted text-center">No public notes found</p>
        </c:if>
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Title</th>
                        <th>Author</th>
                        <th>Visibility</th>
                        <th>Ratings</th>
                        <th>Created</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="note" items="${notes}">
                        <tr>
                            <td>${note.id}</td>
                            <td><strong>${note.title}</strong></td>
                            <td>${note.owner.username}</td>
                            <td>
                                <span class="badge badge-${note.isPublic ? 'success' : 'secondary'}">
                                    ${note.isPublic ? 'Public' : 'Private'}
                                </span>
                            </td>
                            <td>
                                <c:if test="${note.ratingCount > 0}">
                                    <span class="text-warning"><i class="fas fa-star"></i> ${String.format("%.1f", note.averageRating)}</span>
                                    (${note.ratingCount})
                                </c:if>
                                <c:if test="${note.ratingCount == 0}">
                                    <span class="text-muted">-</span>
                                </c:if>
                            </td>
                            <td><small>${note.createdAt}</small></td>
                            <td>
                                <div class="btn-group btn-group-sm">
                                    <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" class="btn btn-outline-primary">
                                        <i class="fas fa-eye"></i>
                                    </a>
                                    <a href="${pageContext.request.contextPath}/admin?action=reassign&id=${note.id}" class="btn btn-outline-warning">
                                        <i class="fas fa-exchange-alt"></i>
                                    </a>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
