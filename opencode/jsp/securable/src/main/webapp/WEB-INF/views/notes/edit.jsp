<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="../layout.jsp">
    <jsp:param name="pageTitle" value="Edit Note"/>
</jsp:include>
<jsp:body>
<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card">
            <div class="card-header">
                <h4 class="mb-0">Edit Note</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger" role="alert">${error}</div>
                </c:if>
                
                <form method="post" action="${pageContext.request.contextPath}/notes">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="id" value="${note.id}">
                    
                    <div class="mb-3">
                        <label for="title" class="form-label">Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="title" name="title" 
                               value="${note.title}" required autofocus>
                    </div>
                    
                    <div class="mb-3">
                        <label for="content" class="form-label">Content <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="content" name="content" rows="10" 
                                  required>${note.content}</textarea>
                    </div>
                    
                    <div class="mb-3 form-check">
                        <input type="checkbox" class="form-check-input" id="isPublic" name="isPublic" 
                               ${note.public ? 'checked' : ''}>
                        <label class="form-check-label" for="isPublic">
                            Make this note public
                        </label>
                        <div class="form-text">Public notes can be viewed by anyone and appear in search results.</div>
                    </div>
                    
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">Save Changes</button>
                        <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" 
                           class="btn btn-secondary">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
</jsp:body>
