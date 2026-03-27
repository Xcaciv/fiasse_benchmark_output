<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="row">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-header bg-primary text-white">
                <h4 class="mb-0"><i class="fas fa-user-cog"></i> Profile Settings</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger" role="alert">
                        <i class="fas fa-exclamation-circle"></i> ${error}
                    </div>
                </c:if>
                
                <c:if test="${not empty success}">
                    <div class="alert alert-success" role="alert">
                        <i class="fas fa-check-circle"></i> ${success}
                    </div>
                </c:if>
                
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="action" value="updateProfile">
                    
                    <div class="form-group">
                        <label for="username"><i class="fas fa-user"></i> Username</label>
                        <input type="text" class="form-control" id="username" name="username" 
                               value="${user.username}" required>
                    </div>
                    
                    <div class="form-group">
                        <label for="email"><i class="fas fa-envelope"></i> Email Address</label>
                        <input type="email" class="form-control" id="email" name="email" 
                               value="${user.email}" required>
                    </div>
                    
                    <div class="form-group">
                        <label><i class="fas fa-shield-alt"></i> Role</label>
                        <input type="text" class="form-control" value="${user.role}" readonly>
                    </div>
                    
                    <div class="form-group">
                        <label><i class="fas fa-calendar"></i> Member Since</label>
                        <input type="text" class="form-control" value="${user.createdAt}" readonly>
                    </div>
                    
                    <button type="submit" class="btn btn-primary">
                        <i class="fas fa-save"></i> Update Profile
                    </button>
                </form>
            </div>
        </div>
    </div>
    
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-header bg-warning text-dark">
                <h4 class="mb-0"><i class="fas fa-key"></i> Change Password</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty passwordError}">
                    <div class="alert alert-danger" role="alert">
                        <i class="fas fa-exclamation-circle"></i> ${passwordError}
                    </div>
                </c:if>
                
                <c:if test="${not empty passwordSuccess}">
                    <div class="alert alert-success" role="alert">
                        <i class="fas fa-check-circle"></i> ${passwordSuccess}
                    </div>
                </c:if>
                
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="action" value="updatePassword">
                    
                    <div class="form-group">
                        <label for="currentPassword"><i class="fas fa-lock"></i> Current Password</label>
                        <input type="password" class="form-control" id="currentPassword" name="currentPassword" required>
                    </div>
                    
                    <div class="form-group">
                        <label for="newPassword"><i class="fas fa-lock"></i> New Password</label>
                        <input type="password" class="form-control" id="newPassword" name="newPassword" required minlength="8">
                        <small class="form-text text-muted">At least 8 characters</small>
                    </div>
                    
                    <div class="form-group">
                        <label for="confirmPassword"><i class="fas fa-lock"></i> Confirm New Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" required>
                    </div>
                    
                    <button type="submit" class="btn btn-warning">
                        <i class="fas fa-key"></i> Change Password
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
