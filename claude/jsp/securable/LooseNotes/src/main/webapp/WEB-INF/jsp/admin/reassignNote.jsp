<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container mt-4" style="max-width: 640px;">
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/users">Users</a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Reassign Note</li>
        </ol>
    </nav>

    <div class="d-flex align-items-center mb-4">
        <h1 class="h3 mb-0 me-2">Reassign Note</h1>
        <span class="badge bg-danger">ADMIN</span>
    </div>

    <%-- Flash messages --%>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <c:out value="${errorMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <c:out value="${successMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm">
        <div class="card-body p-4">
            <dl class="row mb-4">
                <dt class="col-sm-4 text-muted">Note ID</dt>
                <dd class="col-sm-8 fw-semibold">
                    <c:out value="${noteId}"/>
                </dd>
            </dl>

            <hr class="mb-4">

            <form method="post"
                  action="${pageContext.request.contextPath}/admin/reassign/<c:out value="${noteId}"/>"
                  novalidate>
                <input type="hidden" name="_csrf" value="${csrfToken}">

                <div class="mb-4">
                    <label for="newOwnerId" class="form-label fw-semibold">
                        Assign to User <span class="text-danger" aria-hidden="true">*</span>
                    </label>
                    <select id="newOwnerId"
                            name="newOwnerId"
                            class="form-select"
                            required>
                        <option value="" disabled selected>— Select a user —</option>
                        <c:forEach var="user" items="${users}">
                            <option value="<c:out value="${user.id}"/>">
                                <c:out value="${user.username}"/>
                                (<c:out value="${user.email}"/>)
                            </option>
                        </c:forEach>
                    </select>
                    <div class="invalid-feedback">Please select a user to reassign this note to.</div>
                </div>

                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-danger">
                        Reassign Note
                    </button>
                    <a href="${pageContext.request.contextPath}/admin/users"
                       class="btn btn-outline-secondary">
                        Cancel
                    </a>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
