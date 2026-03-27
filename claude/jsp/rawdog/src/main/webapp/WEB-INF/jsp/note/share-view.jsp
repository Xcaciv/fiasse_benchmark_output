<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="${note.title} - Shared Note" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="alert alert-info">
    <i class="bi bi-share"></i> You are viewing a shared note.
    <a href="${pageContext.request.contextPath}/login" class="alert-link">Log in</a> to rate or create your own notes.
</div>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card shadow mb-4">
            <div class="card-header">
                <h3 class="mb-0">${note.title}</h3>
                <small class="text-muted">By ${note.ownerUsername}</small>
            </div>
            <div class="card-body">
                <div class="note-content mb-3" style="white-space: pre-wrap;"><c:out value="${note.content}"/></div>

                <c:if test="${not empty attachments}">
                    <h6><i class="bi bi-paperclip"></i> Attachments</h6>
                    <ul class="list-group list-group-flush mb-3">
                        <c:forEach var="att" items="${attachments}">
                            <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                                <a href="${pageContext.request.contextPath}/files/${att.id}?token=${param.token}" target="_blank">
                                    <i class="bi bi-file-earmark"></i> ${att.originalFilename}
                                </a>
                                <small class="text-muted">${att.fileSizeDisplay}</small>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>
            </div>
        </div>

        <c:if test="${not empty ratings}">
            <div class="card shadow">
                <div class="card-header">
                    <h5 class="mb-0">
                        <i class="bi bi-star"></i> Ratings
                        <span class="ms-2 text-warning">
                            <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1" minFractionDigits="1"/>
                            / 5.0
                        </span>
                        <small class="text-muted">(${note.ratingCount})</small>
                    </h5>
                </div>
                <div class="card-body">
                    <c:forEach var="r" items="${ratings}">
                        <div class="border-bottom pb-2 mb-2">
                            <div class="d-flex justify-content-between">
                                <strong>${r.raterUsername}</strong>
                                <span class="text-warning">
                                    <c:forEach var="s" begin="1" end="${r.rating}">&#9733;</c:forEach>
                                    <c:forEach var="s" begin="${r.rating + 1}" end="5">&#9734;</c:forEach>
                                </span>
                            </div>
                            <c:if test="${not empty r.comment}">
                                <p class="mb-0 text-muted small mt-1"><c:out value="${r.comment}"/></p>
                            </c:if>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </c:if>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
