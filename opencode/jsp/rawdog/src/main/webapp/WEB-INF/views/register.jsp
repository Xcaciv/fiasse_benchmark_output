<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
        <div class="card shadow">
            <div class="card-body p-5">
                <h3 class="text-center mb-4"><i class="fas fa-user-plus text-primary"></i> Register</h3>
                
                <c:if test="${not empty error}">
                    <div class="alert alert-danger" role="alert">
                        <i class="fas fa-exclamation-circle"></i> ${error}
                    </div>
                </c:if>
                
                <form action="${pageContext.request.contextPath}/auth" method="post">
                    <input type="hidden" name="action" value="register">
                    
                    <div class="form-group">
                        <label for="username"><i class="fas fa-user"></i> Username</label>
                        <input type="text" class="form-control" id="username" name="username" required autofocus
                               pattern="[A-Za-z0-9_]{3,50}" title="3-50 characters, letters, numbers, and underscores only">
                        <small class="form-text text-muted">3-50 characters, letters, numbers, and underscores only</small>
                    </div>
                    
                    <div class="form-group">
                        <label for="email"><i class="fas fa-envelope"></i> Email Address</label>
                        <input type="email" class="form-control" id="email" name="email" required>
                    </div>
                    
                    <div class="form-group">
                        <label for="password"><i class="fas fa-lock"></i> Password</label>
                        <input type="password" class="form-control" id="password" name="password" required minlength="8">
                        <small class="form-text text-muted">At least 8 characters</small>
                    </div>
                    
                    <div class="form-group">
                        <label for="confirmPassword"><i class="fas fa-lock"></i> Confirm Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" required>
                    </div>
                    
                    <button type="submit" class="btn btn-primary btn-block">
                        <i class="fas fa-user-plus"></i> Register
                    </button>
                </form>
                
                <hr>
                
                <div class="text-center">
                    <p class="mb-0">Already have an account?</p>
                    <a href="${pageContext.request.contextPath}/auth" class="btn btn-outline-primary">
                        <i class="fas fa-sign-in-alt"></i> Login
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
