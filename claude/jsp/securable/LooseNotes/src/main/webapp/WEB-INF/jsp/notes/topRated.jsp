<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container mt-4">
    <h1 class="h2 mb-4">Top Rated Notes</h1>

    <c:choose>
        <c:when test="${empty topRatedNotes}">
            <div class="text-center py-5">
                <p class="text-muted fs-5">No rated public notes yet.</p>
                <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-primary mt-2">Browse All Notes</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead class="table-light">
                        <tr>
                            <th scope="col">#</th>
                            <th scope="col">Title</th>
                            <th scope="col">Author</th>
                            <th scope="col" class="text-center">Avg Rating</th>
                            <th scope="col" class="text-center">Ratings</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="note" items="${topRatedNotes}" varStatus="status">
                            <tr>
                                <td class="text-muted">${status.index + 1}</td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/notes/${note.id}"
                                       class="text-decoration-none fw-semibold">
                                        <c:out value="${note.title}"/>
                                    </a>
                                </td>
                                <td><c:out value="${note.authorUsername}"/></td>
                                <td class="text-center">
                                    <span class="text-warning me-1">
                                        <c:set var="avgRounded" value="${note.averageRating}"/>
                                        <c:forEach begin="1" end="5" var="star">
                                            <c:choose>
                                                <c:when test="${star <= avgRounded}">&#9733;</c:when>
                                                <c:otherwise>&#9734;</c:otherwise>
                                            </c:choose>
                                        </c:forEach>
                                    </span>
                                    <span class="fw-semibold">
                                        <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1"/>
                                    </span>
                                </td>
                                <td class="text-center text-muted">
                                    <c:out value="${note.ratingCount}"/>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
