<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
        <c:set var="isEdit" value="${mode eq 'edit'}" />
        <c:set var="titleValue" value="${not empty formTitle ? formTitle : (note != null ? note.title : '')}" />
        <c:set var="contentValue" value="${not empty formContent ? formContent : (note != null ? note.content : '')}" />
        <c:set var="publicValue" value="${formPublic ne null ? formPublic : (note != null and note.publicNote)}" />
        <c:choose>
            <c:when test="${isEdit}">
                <c:set var="formAction" value="${pageContext.request.contextPath}/notes/edit?id=${note.id}" />
            </c:when>
            <c:otherwise>
                <c:set var="formAction" value="${pageContext.request.contextPath}/notes/create" />
            </c:otherwise>
        </c:choose>
        <section class="card" style="max-width:860px; margin:0 auto;">
            <h1>${isEdit ? 'Edit note' : 'Create note'}</h1>
            <form method="post" enctype="multipart/form-data" action="${formAction}">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <label>
                    Title
                    <input type="text" name="title" maxlength="150" required value="${fn:escapeXml(titleValue)}">
        </label>
        <label>
            Content
            <textarea name="content" maxlength="20000" required><c:out value="${contentValue}" /></textarea>
        </label>
        <label class="form-check">
            <input type="checkbox" name="isPublic" ${publicValue ? 'checked' : ''}>
            Make this note public
        </label>
        <label>
            Attach files
            <input type="file" name="attachments" multiple accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg">
        </label>
        <p class="muted">Allowed file types: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG. Maximum file size: 5 MB each.</p>
        <c:if test="${isEdit and not empty attachments}">
            <div>
                <strong>Existing attachments</strong>
                <ul>
                    <c:forEach var="attachment" items="${attachments}">
                        <li><a href="${pageContext.request.contextPath}/attachments/download?id=${attachment.id}"><c:out value="${attachment.originalName}" /></a></li>
                    </c:forEach>
                </ul>
            </div>
        </c:if>
        <div class="actions">
            <button class="btn primary" type="submit">${isEdit ? 'Save changes' : 'Create note'}</button>
            <a class="btn ghost" href="${pageContext.request.contextPath}/notes">Cancel</a>
        </div>
    </form>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
