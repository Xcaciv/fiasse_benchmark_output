<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="row justify-content-center mt-5">
    <div class="col-md-8 text-center">
        <h1 class="display-3 fw-bold mb-3">LooseNotes</h1>
        <p class="lead mb-5 text-muted">Your personal note-taking workspace</p>
        <div class="d-flex justify-content-center gap-3">
            <a href="${pageContext.request.contextPath}/auth/login"
               class="btn btn-primary btn-lg px-5">Login</a>
            <a href="${pageContext.request.contextPath}/auth/register"
               class="btn btn-outline-secondary btn-lg px-5">Register</a>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
