<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="row">
    <div class="col-md-8 mx-auto">
        <div class="card shadow">
            <div class="card-header bg-primary text-white">
                <h4 class="mb-0"><i class="fas fa-plus"></i> Create New Note</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger" role="alert">
                        <i class="fas fa-exclamation-circle"></i> ${error}
                    </div>
                </c:if>
                
                <form action="${pageContext.request.contextPath}/notes" method="post">
                    <input type="hidden" name="action" value="create">
                    
                    <div class="form-group">
                        <label for="title"><i class="fas fa-heading"></i> Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="title" name="title" required autofocus
                               placeholder="Enter note title">
                    </div>
                    
                    <div class="form-group">
                        <label for="content"><i class="fas fa-align-left"></i> Content <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="content" name="content" rows="10" required
                                  placeholder="Write your note content here..."></textarea>
                    </div>
                    
                    <div class="form-group">
                        <div class="custom-control custom-switch">
                            <input type="checkbox" class="custom-control-input" id="isPublic" name="isPublic">
                            <label class="custom-control-label" for="isPublic">
                                <i class="fas fa-globe"></i> Make this note public
                            </label>
                        </div>
                        <small class="form-text text-muted">
                            Public notes can be viewed by anyone and appear in search results.
                        </small>
                    </div>
                    
                    <div class="d-flex justify-content-between">
                        <a href="${pageContext.request.contextPath}/notes" class="btn btn-secondary">
                            <i class="fas fa-arrow-left"></i> Cancel
                        </a>
                        <button type="submit" class="btn btn-primary">
                            <i class="fas fa-save"></i> Create Note
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
