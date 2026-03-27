<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Create Note - Loose Notes"/>
</jsp:include>
<div class="note-form">
    <h2>Create New Note</h2>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/notes/create">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <div class="form-group">
            <label for="title">Title (required, max 200 chars)</label>
            <input type="text" id="title" name="title" required maxlength="200" value="<c:out value='${param.title}'/>">
        </div>
        <div class="form-group">
            <label for="content">Content (required, max 50,000 chars)</label>
            <textarea id="content" name="content" required maxlength="50000" rows="15"><c:out value="${param.content}"/></textarea>
        </div>
        <div class="form-group">
            <label for="visibility">Visibility</label>
            <select id="visibility" name="visibility">
                <option value="PRIVATE" selected>Private</option>
                <option value="PUBLIC">Public</option>
            </select>
        </div>
        <button type="submit" class="btn btn-primary">Create Note</button>
        <a href="${pageContext.request.contextPath}/notes/list" class="btn">Cancel</a>
    </form>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
