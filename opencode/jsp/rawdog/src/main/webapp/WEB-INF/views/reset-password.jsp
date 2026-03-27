<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
        <div class="card shadow">
            <div class="card-body p-5">
                <h3 class="text-center mb-4"><i class="fas fa-key text-primary"></i> Set New Password</h3>
                
                <c:if test="${not empty error}">
                    <div class="alert alert-danger" role="alert">
                        <i class="fas fa-exclamation-circle"></i> ${error}
                    </div>
                </c:if>
                
                <form action="${pageContext.request.contextPath}/auth" method="post">
                    <input type="hidden" name="action" value="resetToken">
                    <input type="hidden" name="token" value="${token}">
                    
                    <div class="form-group">
                        <label for="password"><i class="fas fa-lock"></i> New Password</label>
                        <input type="password" class="form-control" id="password" name="password" required minlength="8">
                        <small class="form-text text-muted">At least 8 characters</small>
                    </div>
                    
                    <div class="form-group">
                        <label for="confirmPassword"><i class="fas fa-lock"></i> Confirm New Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" required>
                    </div>
                    
                    <button type="submit" class="btn btn-primary btn-block">
                        <i class="fas fa-save"></i> Reset Password
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
