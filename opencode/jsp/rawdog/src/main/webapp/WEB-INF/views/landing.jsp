<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="jumbotron bg-white text-center">
    <h1 class="display-4"><i class="fas fa-sticky-note text-primary"></i> Welcome to Loose Notes</h1>
    <p class="lead">A multi-user note-taking platform to create, share, and rate notes</p>
    <hr class="my-4">
    <p>Create an account to start taking notes and sharing with others</p>
    <a class="btn btn-primary btn-lg" href="${pageContext.request.contextPath}/auth?action=register" role="button">
        <i class="fas fa-user-plus"></i> Get Started
    </a>
    <a class="btn btn-outline-primary btn-lg" href="${pageContext.request.contextPath}/auth" role="button">
        <i class="fas fa-sign-in-alt"></i> Login
    </a>
</div>

<c:if test="${not empty recentNotes}">
<div class="mt-5">
    <h3 class="mb-4"><i class="fas fa-clock"></i> Recent Public Notes</h3>
    <div class="row">
        <c:forEach var="note" items="${recentNotes}" begin="0" end="5">
            <div class="col-md-6 mb-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">${note.title}</h5>
                        <p class="card-text text-muted">${note.excerpt}</p>
                        <div class="d-flex justify-content-between align-items-center">
                            <small class="text-muted">
                                <i class="fas fa-user"></i> ${note.owner.username}
                            </small>
                            <div>
                                <c:if test="${note.ratingCount > 0}">
                                    <span class="text-warning">
                                        <i class="fas fa-star"></i> ${String.format("%.1f", note.averageRating)} (${note.ratingCount})
                                    </span>
                                </c:if>
                                <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" class="btn btn-sm btn-outline-primary">
                                    View <i class="fas fa-arrow-right"></i>
                                </a>
                            </div>
                        </div>
                    </div>
                    <div class="card-footer text-muted">
                        <small>${note.createdAt}</small>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
</div>
</c:if>

<div class="mt-5">
    <h3 class="mb-4 text-center">Features</h3>
    <div class="row">
        <div class="col-md-4 mb-4">
            <div class="card h-100 border-0 shadow-sm">
                <div class="card-body text-center">
                    <i class="fas fa-lock fa-3x text-primary mb-3"></i>
                    <h5 class="card-title">Private Notes</h5>
                    <p class="card-text">Keep your notes private or share them with specific people</p>
                </div>
            </div>
        </div>
        <div class="col-md-4 mb-4">
            <div class="card h-100 border-0 shadow-sm">
                <div class="card-body text-center">
                    <i class="fas fa-share-alt fa-3x text-success mb-3"></i>
                    <h5 class="card-title">Easy Sharing</h5>
                    <p class="card-text">Generate share links to let others view your notes</p>
                </div>
            </div>
        </div>
        <div class="col-md-4 mb-4">
            <div class="card h-100 border-0 shadow-sm">
                <div class="card-body text-center">
                    <i class="fas fa-star fa-3x text-warning mb-3"></i>
                    <h5 class="card-title">Rate & Review</h5>
                    <p class="card-text">Rate notes and see the top-rated ones</p>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
