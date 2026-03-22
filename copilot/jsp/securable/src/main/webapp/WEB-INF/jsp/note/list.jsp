<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="My Notes"/>
</jsp:include>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h1 class="h3">My Notes</h1>
    <a href="${pageContext.request.contextPath}/notes/new" class="btn btn-primary">+ New Note</a>
</div>

<c:choose>
    <c:when test="${empty notes}">
        <div class="alert alert-info">You haven't created any notes yet.
            <a href="${pageContext.request.contextPath}/notes/new">Create your first note!</a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row g-3">
            <c:forEach var="note" items="${notes}">
                <div class="col-md-6 col-lg-4">
                    <div class="card h-100 shadow-sm">
                        <div class="card-body">
                            <div class="d-flex justify-content-between">
                                <h5 class="card-title">
                                    <a href="${pageContext.request.contextPath}/notes/${note.id}" class="text-decoration-none">
                                        <c:out value="${note.title}"/>
                                    </a>
                                </h5>
                                <span class="badge ${note.visibility == 'PUBLIC' ? 'bg-success' : 'bg-secondary'}">
                                    <c:out value="${note.visibility}"/>
                                </span>
                            </div>
                            <p class="card-text text-muted small">
                                <c:out value="${note.excerpt}"/>
                            </p>
                        </div>
                        <div class="card-footer small text-muted d-flex justify-content-between">
                            <span>${note.updatedAt}</span>
                            <div>
                                <a href="${pageContext.request.contextPath}/notes/${note.id}/edit" class="btn btn-sm btn-outline-secondary">Edit</a>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
