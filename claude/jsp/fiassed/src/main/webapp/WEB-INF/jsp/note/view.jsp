<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<article class="note-view">
    <div class="note-header">
        <h1><c:out value="${note.title}"/></h1>
        <div class="note-meta">
            <c:choose>
                <c:when test="${note.visibility == 'PUBLIC'}">
                    <span class="badge badge-public">Public</span>
                </c:when>
                <c:otherwise>
                    <span class="badge badge-private">Private</span>
                </c:otherwise>
            </c:choose>
            <span class="note-dates">
                Created: <c:out value="${note.createdAt}"/>
                <c:if test="${not empty note.updatedAt}">
                    &mdash; Updated: <c:out value="${note.updatedAt}"/>
                </c:if>
            </span>
        </div>

        <c:if test="${sessionScope.userId == note.userId}">
            <div class="note-actions">
                <a href="${pageContext.request.contextPath}/notes/${note.id}/edit" class="btn btn-primary">Edit</a>
                <a href="${pageContext.request.contextPath}/notes/${note.id}/delete" class="btn btn-danger">Delete</a>
            </div>
        </c:if>
    </div>

    <div class="note-content">
        <%-- c:out escapes all HTML — XSS prevention (F-04, GR output encoding) --%>
        <pre><c:out value="${note.content}"/></pre>
    </div>

    <%-- Attachments Section --%>
    <section class="note-section" id="attachments">
        <h2>Attachments</h2>

        <c:choose>
            <c:when test="${not empty attachments}">
                <ul class="attachment-list">
                    <c:forEach var="att" items="${attachments}">
                        <li>
                            <%-- Download URL uses internal ID, never the filename --%>
                            <a href="${pageContext.request.contextPath}/attachments/${att.id}">
                                <c:out value="${att.originalFilename}"/>
                            </a>
                            <span class="attachment-size">(<c:out value="${att.fileSize}"/> bytes)</span>
                            <c:if test="${sessionScope.userId == note.userId}">
                                <form method="post"
                                      action="${pageContext.request.contextPath}/attachments/delete/${att.id}"
                                      class="inline-form">
                                    <input type="hidden" name="_csrf" value="${csrfToken}"/>
                                    <button type="submit" class="btn btn-small btn-danger"
                                            onclick="return confirm('Remove this attachment?')">Remove</button>
                                </form>
                            </c:if>
                        </li>
                    </c:forEach>
                </ul>
            </c:when>
            <c:otherwise>
                <p class="empty-text">No attachments.</p>
            </c:otherwise>
        </c:choose>

        <c:if test="${sessionScope.userId == note.userId}">
            <form method="post"
                  action="${pageContext.request.contextPath}/attachments/upload/${note.id}"
                  enctype="multipart/form-data"
                  class="upload-form">
                <input type="hidden" name="_csrf" value="${csrfToken}"/>
                <div class="form-group form-inline">
                    <label for="file">Upload File (PDF, DOC, DOCX, TXT, PNG, JPG — max 10 MB)</label>
                    <input type="file" id="file" name="file" required
                           accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg"/>
                    <button type="submit" class="btn btn-primary btn-small">Upload</button>
                </div>
            </form>
        </c:if>
    </section>

    <%-- Ratings Section --%>
    <section class="note-section" id="ratings">
        <h2>Ratings</h2>

        <div class="average-rating">
            Average:
            <strong>
                <c:choose>
                    <c:when test="${not empty note.averageRating}">
                        <c:out value="${note.averageRating}"/> / 5
                    </c:when>
                    <c:otherwise>No ratings yet</c:otherwise>
                </c:choose>
            </strong>
            (<c:out value="${note.ratingCount}"/> rating<c:if test="${note.ratingCount != 1}">s</c:if>)
        </div>

        <%-- Owner sees full rating list (F-11) --%>
        <c:if test="${sessionScope.userId == note.userId && not empty ratings}">
            <table class="data-table ratings-table">
                <thead>
                    <tr>
                        <th>Rating</th>
                        <th>Comment</th>
                        <th>By</th>
                        <th>Date</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="rating" items="${ratings}">
                        <tr>
                            <td><c:out value="${rating.ratingValue}"/> / 5</td>
                            <%-- comment may be null --%>
                            <td><c:out value="${rating.comment}"/></td>
                            <td><c:out value="${rating.username}"/></td>
                            <td><c:out value="${rating.createdAt}"/></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:if>

        <%-- Rating form for authenticated non-owners (F-10) --%>
        <c:if test="${not empty sessionScope.userId && sessionScope.userId != note.userId}">
            <div class="rating-form-container">
                <h3><c:choose>
                    <c:when test="${not empty existingRating}">Update Your Rating</c:when>
                    <c:otherwise>Leave a Rating</c:otherwise>
                </c:choose></h3>
                <form method="post"
                      action="${pageContext.request.contextPath}/ratings/submit/${note.id}">
                    <input type="hidden" name="_csrf" value="${csrfToken}"/>
                    <div class="form-group">
                        <label for="ratingValue">Rating (1–5)</label>
                        <select id="ratingValue" name="ratingValue" required>
                            <option value="">-- Select --</option>
                            <option value="1">1 - Poor</option>
                            <option value="2">2 - Fair</option>
                            <option value="3">3 - Good</option>
                            <option value="4">4 - Very Good</option>
                            <option value="5">5 - Excellent</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="comment">Comment (optional, max 1000 chars)</label>
                        <textarea id="comment" name="comment" rows="3" maxlength="1000"></textarea>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">Submit Rating</button>
                    </div>
                </form>
            </div>
        </c:if>
    </section>

    <%-- Share Links Section (owner only, F-08) --%>
    <c:if test="${sessionScope.userId == note.userId}">
        <section class="note-section" id="share">
            <h2>Share Links</h2>

            <%-- One-time token display: set in session after generation, cleared after first render --%>
            <c:if test="${not empty sessionScope.flashShareToken}">
                <div class="alert alert-info">
                    <strong>Share link created.</strong> Copy this URL now — it will not be shown again:
                    <br/>
                    <code class="share-url">
                        <c:out value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/share/${sessionScope.flashShareToken}"/>
                    </code>
                </div>
                <%-- Clear flash token from session after display --%>
                <c:remove var="flashShareToken" scope="session"/>
            </c:if>

            <c:choose>
                <c:when test="${not empty shareLinks}">
                    <ul class="share-link-list">
                        <c:forEach var="link" items="${shareLinks}">
                            <li>
                                <span class="share-link-id">Link #<c:out value="${link.id}"/></span>
                                <span class="share-created">Created: <c:out value="${link.createdAt}"/></span>
                                <form method="post"
                                      action="${pageContext.request.contextPath}/notes/${note.id}/share/${link.id}/revoke"
                                      class="inline-form">
                                    <input type="hidden" name="_csrf" value="${csrfToken}"/>
                                    <button type="submit" class="btn btn-small btn-danger">Revoke</button>
                                </form>
                            </li>
                        </c:forEach>
                    </ul>
                </c:when>
                <c:otherwise>
                    <p class="empty-text">No active share links.</p>
                </c:otherwise>
            </c:choose>

            <form method="post"
                  action="${pageContext.request.contextPath}/notes/${note.id}/share">
                <input type="hidden" name="_csrf" value="${csrfToken}"/>
                <button type="submit" class="btn btn-primary">Generate Share Link</button>
            </form>
        </section>
    </c:if>
</article>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
