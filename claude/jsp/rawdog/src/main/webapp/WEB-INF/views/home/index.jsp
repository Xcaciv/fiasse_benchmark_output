<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Welcome" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-8 text-center py-5">
        <i class="bi bi-journal-text text-primary" style="font-size: 5rem;"></i>
        <h1 class="display-4 fw-bold mt-3">Loose Notes</h1>
        <p class="lead text-muted mt-3">
            A multi-user platform for creating, sharing, and rating notes.
            Organize your thoughts, collaborate with others, and discover great content.
        </p>
        <div class="mt-4 d-flex gap-3 justify-content-center">
            <a href="${pageContext.request.contextPath}/register" class="btn btn-primary btn-lg">
                <i class="bi bi-person-plus"></i> Get Started
            </a>
            <a href="${pageContext.request.contextPath}/login" class="btn btn-outline-primary btn-lg">
                <i class="bi bi-box-arrow-in-right"></i> Sign In
            </a>
        </div>
    </div>
</div>

<div class="row mt-4 g-4">
    <div class="col-md-4">
        <div class="card h-100 shadow-sm">
            <div class="card-body text-center p-4">
                <i class="bi bi-pencil-square text-primary" style="font-size: 2.5rem;"></i>
                <h5 class="card-title mt-3">Create Notes</h5>
                <p class="card-text text-muted">
                    Write and organize your notes with rich text content and file attachments.
                </p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card h-100 shadow-sm">
            <div class="card-body text-center p-4">
                <i class="bi bi-share text-success" style="font-size: 2.5rem;"></i>
                <h5 class="card-title mt-3">Share Easily</h5>
                <p class="card-text text-muted">
                    Generate unique share links so anyone can view your notes without signing up.
                </p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card h-100 shadow-sm">
            <div class="card-body text-center p-4">
                <i class="bi bi-star text-warning" style="font-size: 2.5rem;"></i>
                <h5 class="card-title mt-3">Rate & Discover</h5>
                <p class="card-text text-muted">
                    Rate notes from 1-5 stars and discover top-rated content from the community.
                </p>
            </div>
        </div>
    </div>
</div>

<div class="text-center mt-5">
    <a href="${pageContext.request.contextPath}/top-rated" class="btn btn-outline-secondary">
        <i class="bi bi-trophy"></i> Browse Top Rated Notes
    </a>
    <a href="${pageContext.request.contextPath}/search" class="btn btn-outline-secondary ms-2">
        <i class="bi bi-search"></i> Search Notes
    </a>
</div>

<%@ include file="../shared/footer.jsp" %>
