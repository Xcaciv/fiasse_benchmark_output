<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="row">
    <div class="col-md-6 mx-auto">
        <div class="card shadow border-danger">
            <div class="card-header bg-danger text-white">
                <h4 class="mb-0"><i class="fas fa-exclamation-triangle"></i> Confirm Delete</h4>
            </div>
            <div class="card-body text-center">
                <i class="fas fa-trash fa-5x text-danger mb-4"></i>
                <h5>Are you sure you want to delete this note?</h5>
                <div class="alert alert-warning">
                    <h6 class="mb-1">${note.title}</h6>
                    <small>This will permanently delete the note and all its attachments and ratings.</small>
                </div>
                
                <form action="${pageContext.request.contextPath}/notes" method="post">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="${note.id}">
                    
                    <div class="d-flex justify-content-center">
                        <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" class="btn btn-secondary mr-2">
                            <i class="fas fa-arrow-left"></i> Cancel
                        </a>
                        <button type="submit" class="btn btn-danger">
                            <i class="fas fa-trash"></i> Delete Permanently
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
