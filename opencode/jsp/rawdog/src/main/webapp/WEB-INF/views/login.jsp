<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
        <div class="card shadow">
            <div class="card-body p-5">
                <h3 class="text-center mb-4"><i class="fas fa-sign-in-alt text-primary"></i> Login</h3>
                
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
                
                <form action="${pageContext.request.contextPath}/auth" method="post">
                    <input type="hidden" name="action" value="login">
                    
                    <div class="form-group">
                        <label for="username"><i class="fas fa-user"></i> Username</label>
                        <input type="text" class="form-control" id="username" name="username" required autofocus>
                    </div>
                    
                    <div class="form-group">
                        <label for="password"><i class="fas fa-lock"></i> Password</label>
                        <input type="password" class="form-control" id="password" name="password" required>
                    </div>
                    
                    <button type="submit" class="btn btn-primary btn-block">
                        <i class="fas fa-sign-in-alt"></i> Login
                    </button>
                </form>
                
                <div class="text-center mt-3">
                    <a href="${pageContext.request.contextPath}/auth?action=resetRequest">
                        <i class="fas fa-key"></i> Forgot Password?
                    </a>
                </div>
                
                <hr>
                
                <div class="text-center">
                    <p class="mb-0">Don't have an account?</p>
                    <a href="${pageContext.request.contextPath}/auth?action=register" class="btn btn-outline-primary">
                        <i class="fas fa-user-plus"></i> Register
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
