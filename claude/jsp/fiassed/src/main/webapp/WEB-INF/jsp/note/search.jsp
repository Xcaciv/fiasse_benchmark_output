<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>Search Notes</h1>
</div>

<form method="get" action="${pageContext.request.contextPath}/search" class="search-form">
    <div class="search-bar">
        <input type="text"
               id="q"
               name="q"
               placeholder="Search notes..."
               maxlength="255"
               value="${fn:escapeXml(query)}"/>
        <button type="submit" class="btn btn-primary">Search</button>
    </div>
</form>

<c:if test="${not empty query}">
    <div class="search-results">
        <c:choose>
            <c:when test="${not empty notes}">
                <p class="results-count">
                    Found <strong><c:out value="${fn:length(notes)}"/></strong> result(s) for
                    &ldquo;<c:out value="${query}"/>&rdquo;
                </p>

                <div class="note-card-list">
                    <c:forEach var="note" items="${notes}">
                        <div class="card note-card">
                            <h3 class="card-title">
                                <a href="${pageContext.request.contextPath}/notes/${note.id}">
                                    <c:out value="${note.title}"/>
                                </a>
                            </h3>
                            <div class="card-meta">
                                <span>By: <c:out value="${note.authorUsername}"/></span>
                                <span>Date: <c:out value="${note.createdAt}"/></span>
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
                <p class="empty-state">
                    No results found for &ldquo;<c:out value="${query}"/>&rdquo;.
                </p>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
