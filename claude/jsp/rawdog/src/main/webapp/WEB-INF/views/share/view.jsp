<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${note.title}" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="alert alert-info mb-4">
            <i class="bi bi-share"></i> This note was shared with you via a share link.
            <c:if test="${empty sessionScope.currentUser}">
                <a href="${pageContext.request.contextPath}/register" class="alert-link ms-1">Create an account</a>
                to write your own notes!
            </c:if>
        </div>

        <div class="d-flex justify-content-between align-items-start mb-3">
            <div>
                <h2 class="mb-1">${note.title}</h2>
                <p class="text-muted small">
                    By <strong>${note.ownerUsername}</strong>
                    <c:if test="${not empty note.createdAt}">
                        &middot; ${note.createdAtDisplay}
                    </c:if>
                </p>
            </div>
        </div>

        <div class="card shadow-sm mb-4">
            <div class="card-body">
                <pre class="note-content">${note.content}</pre>
            </div>
        </div>

        <c:if test="${not empty attachments}">
            <div class="card shadow-sm mb-4">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-paperclip"></i> Attachments</h6>
                </div>
                <div class="list-group list-group-flush">
                    <c:forEach var="att" items="${attachments}">
                        <div class="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <i class="bi bi-file-earmark"></i>
                                <span class="ms-1">${att.originalFilename}</span>
                                <small class="text-muted ms-2">${att.fileSizeDisplay}</small>
                            </div>
                            <a href="${pageContext.request.contextPath}/attachments/${att.id}/download"
                               class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-download"></i> Download
                            </a>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </c:if>

        <div class="text-center mt-4">
            <a href="${pageContext.request.contextPath}/" class="btn btn-outline-primary">
                <i class="bi bi-journal-text"></i> Explore Loose Notes
            </a>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
