<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>Reassign Note</h1>
    <a href="${pageContext.request.contextPath}/admin" class="btn btn-secondary">Back to Dashboard</a>
</div>

<div class="card">
    <div class="reassign-info">
        <p>
            <strong>Note:</strong> <c:out value="${note.title}"/>
        </p>
        <p>
            <strong>Current Owner:</strong> <c:out value="${currentOwner.username}"/>
        </p>
    </div>

    <div class="alert alert-error">
        <strong>Warning:</strong> Reassigning this note will transfer ownership to the selected user.
        The new owner will have full control over the note and all its attachments and share links.
    </div>

    <form method="post" action="${pageContext.request.contextPath}/admin/reassign/${note.id}" novalidate>
        <input type="hidden" name="_csrf" value="${csrfToken}"/>

        <div class="form-group">
            <label for="newOwnerId">New Owner</label>
            <select id="newOwnerId" name="newOwnerId" required>
                <option value="">-- Select a user --</option>
                <c:forEach var="user" items="${users}">
                    <c:if test="${user.id != note.userId}">
                        <option value="${user.id}">
                            <c:out value="${user.username}"/>
                            (<c:out value="${user.email}"/>)
                        </option>
                    </c:if>
                </c:forEach>
            </select>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-danger">Confirm Reassignment</button>
            <a href="${pageContext.request.contextPath}/admin" class="btn btn-secondary">Cancel</a>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
