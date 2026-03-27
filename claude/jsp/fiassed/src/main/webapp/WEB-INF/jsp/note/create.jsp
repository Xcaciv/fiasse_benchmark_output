<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>Create Note</h1>
</div>

<form method="post" action="${pageContext.request.contextPath}/notes/create" novalidate>
    <input type="hidden" name="_csrf" value="${csrfToken}"/>

    <div class="form-group">
        <label for="title">Title</label>
        <input type="text"
               id="title"
               name="title"
               required
               maxlength="500"
               placeholder="Note title"/>
    </div>

    <div class="form-group">
        <label for="content">Content</label>
        <textarea id="content"
                  name="content"
                  rows="16"
                  placeholder="Write your note here..."></textarea>
    </div>

    <div class="form-group">
        <label for="visibility">Visibility</label>
        <select id="visibility" name="visibility">
            <option value="PRIVATE" selected>Private</option>
            <option value="PUBLIC">Public</option>
        </select>
    </div>

    <div class="form-actions">
        <button type="submit" class="btn btn-primary">Create Note</button>
        <a href="${pageContext.request.contextPath}/notes" class="btn btn-secondary">Cancel</a>
    </div>
</form>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
