<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
        <div class="card shadow">
            <div class="card-body p-5">
                <h3 class="text-center mb-4"><i class="fas fa-key text-primary"></i> Reset Password</h3>
                
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
                
                <p class="text-muted text-center">Enter your email address and we'll send you a link to reset your password.</p>
                
                <form action="${pageContext.request.contextPath}/auth" method="post">
                    <input type="hidden" name="action" value="resetRequest">
                    
                    <div class="form-group">
                        <label for="email"><i class="fas fa-envelope"></i> Email Address</label>
                        <input type="email" class="form-control" id="email" name="email" required autofocus>
                    </div>
                    
                    <button type="submit" class="btn btn-primary btn-block">
                        <i class="fas fa-paper-plane"></i> Send Reset Link
                    </button>
                </form>
                
                <hr>
                
                <div class="text-center">
                    <a href="${pageContext.request.contextPath}/auth" class="btn btn-outline-secondary">
                        <i class="fas fa-arrow-left"></i> Back to Login
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
