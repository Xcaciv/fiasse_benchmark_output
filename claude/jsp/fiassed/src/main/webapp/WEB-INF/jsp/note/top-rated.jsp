<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>Top Rated Notes</h1>
    <p class="subtitle">The highest-rated public notes in the community.</p>
</div>

<c:choose>
    <c:when test="${not empty notes}">
        <div class="note-card-list">
            <c:forEach var="note" items="${notes}">
                <div class="card note-card">
                    <div class="card-header">
                        <h3 class="card-title">
                            <a href="${pageContext.request.contextPath}/notes/${note.id}">
                                <c:out value="${note.title}"/>
                            </a>
                        </h3>
                        <div class="rating-summary">
                            <span class="rating-score">
                                &#9733; <c:out value="${note.averageRating}"/>
                            </span>
                            <span class="rating-count">
                                (<c:out value="${note.ratingCount}"/>
                                rating<c:if test="${note.ratingCount != 1}">s</c:if>)
                            </span>
                        </div>
                    </div>

                    <div class="card-meta">
                        <span>By: <c:out value="${note.authorUsername}"/></span>
                    </div>

                    <p class="card-excerpt">
                        <c:set var="contentStr" value="${note.content}"/>
                        <c:choose>
                            <c:when test="${fn:length(contentStr) > 200}">
                                <c:out value="${fn:substring(contentStr, 0, 200)}"/>&#8230;
                            </c:when>
                            <c:otherwise>
                                <c:out value="${contentStr}"/>
                            </c:otherwise>
                        </c:choose>
                    </p>
                </div>
            </c:forEach>
        </div>
    </c:when>
    <c:otherwise>
        <div class="empty-state">
            <p>No rated notes yet. Be the first to rate a public note!</p>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
