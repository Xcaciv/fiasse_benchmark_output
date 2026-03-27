<%--
  Note list - shows user's own notes.
  SSEM: Integrity - output escaped via c:out.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="My Notes - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h1 class="h3">My Notes</h1>
    <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">
        + New Note
    </a>
</div>

<c:if test="${error != null}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<c:choose>
    <c:when test="${empty notes}">
        <div class="text-center py-5 text-muted">
            <p class="lead">You haven't created any notes yet.</p>
            <a href="${pageContext.request.contextPath}/notes/create"
               class="btn btn-outline-primary">Create your first note</a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row row-cols-1 row-cols-md-2 g-4">
            <c:forEach var="note" items="${notes}">
                <div class="col">
                    <div class="card h-100 shadow-sm">
                        <div class="card-body">
                            <div class="d-flex justify-content-between">
                                <h5 class="card-title">
                                    <a href="${pageContext.request.contextPath}/notes/view/<c:out value='${note.id}'/>">
                                        <c:out value="${note.title}"/>
                                    </a>
                                </h5>
                                <span class="badge ${note.public ? 'bg-success' : 'bg-secondary'}">
                                    <c:out value="${note.public ? 'Public' : 'Private'}"/>
                                </span>
                            </div>
                            <p class="card-text text-muted small">
                                <c:out value="${note.getExcerpt(150)}"/>
                            </p>
                        </div>
                        <div class="card-footer d-flex justify-content-between align-items-center">
                            <small class="text-muted">
                                <fmt:formatDate value="${note.createdAt}" pattern="MMM d, yyyy"/>
                            </small>
                            <div>
                                <a href="${pageContext.request.contextPath}/notes/edit/<c:out value='${note.id}'/>"
                                   class="btn btn-sm btn-outline-secondary">Edit</a>
                                <a href="${pageContext.request.contextPath}/notes/delete/<c:out value='${note.id}'/>"
                                   class="btn btn-sm btn-outline-danger ms-1">Delete</a>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
