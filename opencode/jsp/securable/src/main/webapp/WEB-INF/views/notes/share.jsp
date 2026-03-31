<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="../layout.jsp">
    <jsp:param name="pageTitle" value="Share Note"/>
</jsp:include>
<jsp:body>
<div class="row">
    <div class="col-lg-8">
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/notes">My Notes</a></li>
                <li class="breadcrumb-item active">Share</li>
            </ol>
        </nav>
        
        <div class="card">
            <div class="card-header">
                <h4 class="mb-0">Share Note: ${note.title}</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger" role="alert">${error}</div>
                </c:if>
                
                <p>Generate a share link to allow anyone to view this note without logging in.</p>
                
                <c:choose>
                    <c:when test="${not empty shareLink}">
                        <div class="alert alert-success">
                            <h5>Share Link Active</h5>
                            <p class="mb-2">Anyone with this link can view your note:</p>
                            <div class="input-group">
                                <input type="text" class="form-control" id="shareLink" 
                                       value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/share?token=${shareLink.token}" 
                                       readonly>
                                <button class="btn btn-outline-secondary" onclick="copyShareLink()">
                                    <i class="bi bi-clipboard"></i> Copy
                                </button>
                            </div>
                            <c:if test="${shareLink.expiresAt ne null}">
                                <small class="text-muted mt-2 d-block">
                                    Expires: ${shareLink.expiresAt}
                                </small>
                            </c:if>
                        </div>
                        
                        <div class="d-flex gap-2 mt-3">
                            <form method="post" action="${pageContext.request.contextPath}/share">
                                <input type="hidden" name="action" value="regenerate">
                                <input type="hidden" name="id" value="${note.id}">
                                <button type="submit" class="btn btn-warning">
                                    <i class="bi bi-arrow-repeat"></i> Regenerate Link
                                </button>
                            </form>
                            <form method="post" action="${pageContext.request.contextPath}/share">
                                <input type="hidden" name="action" value="revoke">
                                <input type="hidden" name="id" value="${note.id}">
                                <input type="hidden" name="linkId" value="${shareLink.id}">
                                <button type="submit" class="btn btn-danger"
                                        onclick="return confirm('Revoke this share link?')">
                                    <i class="bi bi-x-circle"></i> Revoke Link
                                </button>
                            </form>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="alert alert-info">
                            <p class="mb-0">No active share link for this note. Generate one to share your note!</p>
                        </div>
                        
                        <form method="post" action="${pageContext.request.contextPath}/share">
                            <input type="hidden" name="action" value="generate">
                            <input type="hidden" name="id" value="${note.id}">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-link-45deg"></i> Generate Share Link
                            </button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<script>
function copyShareLink() {
    var copyText = document.getElementById("shareLink");
    copyText.select();
    copyText.setSelectionRange(0, 99999);
    navigator.clipboard.writeText(copyText.value);
    alert("Link copied to clipboard!");
}
</script>
</jsp:body>
