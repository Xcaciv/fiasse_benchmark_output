<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="row">
    <div class="col-md-8 mx-auto">
        <div class="card shadow">
            <div class="card-header bg-warning text-dark">
                <h4 class="mb-0"><i class="fas fa-edit"></i> Edit Note</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger" role="alert">
                        <i class="fas fa-exclamation-circle"></i> ${error}
                    </div>
                </c:if>
                
                <form action="${pageContext.request.contextPath}/notes" method="post">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="id" value="${note.id}">
                    
                    <div class="form-group">
                        <label for="title"><i class="fas fa-heading"></i> Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="title" name="title" required autofocus
                               value="${note.title}">
                    </div>
                    
                    <div class="form-group">
                        <label for="content"><i class="fas fa-align-left"></i> Content <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="content" name="content" rows="10" required>${note.content}</textarea>
                    </div>
                    
                    <div class="form-group">
                        <div class="custom-control custom-switch">
                            <input type="checkbox" class="custom-control-input" id="isPublic" name="isPublic"
                                   ${note.isPublic ? 'checked' : ''}>
                            <label class="custom-control-label" for="isPublic">
                                <i class="fas fa-globe"></i> Make this note public
                            </label>
                        </div>
                        <small class="form-text text-muted">
                            Public notes can be viewed by anyone and appear in search results.
                        </small>
                    </div>
                    
                    <c:if test="${not empty attachments}">
                        <div class="form-group">
                            <label><i class="fas fa-paperclip"></i> Current Attachments</label>
                            <ul class="list-group">
                                <c:forEach var="attachment" items="${attachments}">
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span><i class="fas fa-file"></i> ${attachment.originalFilename}</span>
                                        <a href="${pageContext.request.contextPath}/upload?action=delete&noteId=${note.id}&attachmentId=${attachment.id}" 
                                           class="btn btn-sm btn-outline-danger">
                                            <i class="fas fa-trash"></i>
                                        </a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>
                    
                    <div class="d-flex justify-content-between">
                        <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" class="btn btn-secondary">
                            <i class="fas fa-arrow-left"></i> Cancel
                        </a>
                        <button type="submit" class="btn btn-primary">
                            <i class="fas fa-save"></i> Update Note
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
