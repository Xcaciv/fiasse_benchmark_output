<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>My Notes</h1>
    <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">Create Note</a>
</div>

<c:choose>
    <c:when test="${not empty notes}">
        <table class="data-table">
            <thead>
                <tr>
                    <th>Title</th>
                    <th>Visibility</th>
                    <th>Created</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="note" items="${notes}">
                    <tr>
                        <td>
                            <a href="${pageContext.request.contextPath}/notes/${note.id}">
                                <c:out value="${note.title}"/>
                            </a>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${note.visibility == 'PUBLIC'}">
                                    <span class="badge badge-public">Public</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-private">Private</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:out value="${note.createdAt}"/>
                        </td>
                        <td>
                            <a href="${pageContext.request.contextPath}/notes/${note.id}/edit" class="btn btn-small">Edit</a>
                            <a href="${pageContext.request.contextPath}/notes/${note.id}/delete" class="btn btn-small btn-danger">Delete</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <div class="pagination">
            <c:if test="${page > 1}">
                <a href="${pageContext.request.contextPath}/notes?page=${page - 1}" class="btn btn-small">&laquo; Previous</a>
            </c:if>
            <span class="page-indicator">Page <c:out value="${page}"/></span>
            <c:if test="${fn:length(notes) >= 10}">
                <a href="${pageContext.request.contextPath}/notes?page=${page + 1}" class="btn btn-small">Next &raquo;</a>
            </c:if>
        </div>
    </c:when>
    <c:otherwise>
        <div class="empty-state">
            <p>You have no notes yet.</p>
            <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">Create your first note</a>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
