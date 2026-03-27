<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="row">
    <div class="col-md-8 mx-auto">
        <div class="card shadow border-warning">
            <div class="card-header bg-warning text-dark">
                <h4 class="mb-0"><i class="fas fa-exchange-alt"></i> Reassign Note Ownership</h4>
            </div>
            <div class="card-body">
                <div class="alert alert-info">
                    <h5>Note Details</h5>
                    <p class="mb-1"><strong>Title:</strong> ${note.title}</p>
                    <p class="mb-1"><strong>Current Owner:</strong> ${note.owner.username}</p>
                    <p class="mb-0"><strong>Created:</strong> ${note.createdAt}</p>
                </div>
                
                <form action="${pageContext.request.contextPath}/admin" method="post">
                    <input type="hidden" name="action" value="reassign">
                    <input type="hidden" name="noteId" value="${note.id}">
                    
                    <div class="form-group">
                        <label for="newOwnerId"><i class="fas fa-user"></i> New Owner</label>
                        <select class="form-control" id="newOwnerId" name="newOwnerId" required>
                            <option value="">-- Select New Owner --</option>
                            <c:forEach var="user" items="${users}">
                                <option value="${user.id}" ${user.id == note.userId ? 'selected' : ''}>
                                    ${user.username} (${user.email})
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    
                    <div class="d-flex justify-content-between">
                        <a href="${pageContext.request.contextPath}/admin?action=notes" class="btn btn-secondary">
                            <i class="fas fa-arrow-left"></i> Cancel
                        </a>
                        <button type="submit" class="btn btn-warning">
                            <i class="fas fa-exchange-alt"></i> Reassign Ownership
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
