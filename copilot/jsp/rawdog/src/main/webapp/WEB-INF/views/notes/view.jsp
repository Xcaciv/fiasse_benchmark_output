<%@ page import="java.util.List,com.loosenotes.model.Note,com.loosenotes.model.User,com.loosenotes.model.Attachment,com.loosenotes.model.Rating,com.loosenotes.model.ShareLink,com.loosenotes.util.CsrfUtil,com.loosenotes.util.HtmlUtil,com.loosenotes.util.TimeUtil" %>
<%@ include file="../layout/header.jspf" %>
<%
    Note note = (Note) request.getAttribute("note");
    User owner = (User) request.getAttribute("owner");
    List<Attachment> attachments = (List<Attachment>) request.getAttribute("attachments");
    List<Rating> ratings = (List<Rating>) request.getAttribute("ratings");
    Double averageRating = (Double) request.getAttribute("averageRating");
    Boolean canManage = (Boolean) request.getAttribute("canManage");
    Boolean isOwner = (Boolean) request.getAttribute("isOwner");
    ShareLink shareLink = (ShareLink) request.getAttribute("shareLink");
    Rating userRating = (Rating) request.getAttribute("userRating");
    String shareToken = (String) request.getAttribute("shareToken");
    User signedIn = (User) session.getAttribute("authUser");
    String shareUrl = shareLink == null ? null : request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort()) + request.getContextPath() + "/share?token=" + shareLink.getToken();
%>
<div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
    <div>
        <div class="d-flex align-items-center gap-2 mb-2">
            <h1 class="h2 mb-0"><%= HtmlUtil.escape(note.getTitle()) %></h1>
            <span class="badge <%= note.isPublic() ? "bg-success" : "bg-secondary" %>"><%= note.isPublic() ? "Public" : "Private" %></span>
        </div>
        <p class="muted-small mb-0">By <strong><%= owner == null ? "Unknown" : HtmlUtil.escape(owner.getUsername()) %></strong> · Created <%= TimeUtil.format(note.getCreatedAt()) %> · Updated <%= TimeUtil.format(note.getUpdatedAt()) %></p>
    </div>
    <% if (Boolean.TRUE.equals(canManage)) { %>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-primary" href="<%= request.getContextPath() %>/notes/edit?id=<%= note.getId() %>">Edit</a>
            <form action="<%= request.getContextPath() %>/notes/delete" method="post" onsubmit="return confirm('Delete this note permanently?');">
                <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                <input type="hidden" name="id" value="<%= note.getId() %>">
                <button class="btn btn-outline-danger" type="submit">Delete</button>
            </form>
        </div>
    <% } %>
</div>
<div class="row g-4">
    <div class="col-lg-8">
        <div class="card shadow-sm mb-4">
            <div class="card-body note-content"><%= HtmlUtil.nl2br(note.getContent()) %></div>
        </div>
        <div class="card shadow-sm mb-4">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h2 class="h4 mb-0">Ratings</h2>
                    <span class="badge bg-info text-dark"><%= String.format("%.1f", averageRating == null ? 0.0 : averageRating) %>/5</span>
                </div>
                <% if (signedIn != null) { %>
                    <form action="<%= request.getContextPath() %>/ratings/save" method="post" class="d-grid gap-3 mb-4">
                        <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                        <input type="hidden" name="noteId" value="<%= note.getId() %>">
                        <% if (shareToken != null && !shareToken.isBlank()) { %>
                            <input type="hidden" name="shareToken" value="<%= HtmlUtil.escape(shareToken) %>">
                        <% } %>
                        <div>
                            <label class="form-label" for="rating">Your rating</label>
                            <select class="form-select" id="rating" name="rating">
                                <% for (int i = 1; i <= 5; i++) { %>
                                    <option value="<%= i %>" <%= userRating != null && userRating.getRating() == i ? "selected" : "" %>><%= i %> star(s)</option>
                                <% } %>
                            </select>
                        </div>
                        <div>
                            <label class="form-label" for="comment">Comment</label>
                            <textarea class="form-control" id="comment" name="comment" rows="3"><%= HtmlUtil.escape(userRating == null || userRating.getComment() == null ? "" : userRating.getComment()) %></textarea>
                        </div>
                        <button class="btn btn-primary" type="submit"><%= userRating == null ? "Submit rating" : "Update rating" %></button>
                    </form>
                <% } else { %>
                    <div class="alert alert-secondary">Sign in to rate and comment on this note.</div>
                <% } %>
                <% if (ratings == null || ratings.isEmpty()) { %>
                    <div class="text-muted">No ratings yet.</div>
                <% } else { %>
                    <div class="list-group">
                        <% for (Rating rating : ratings) { %>
                            <div class="list-group-item">
                                <div class="d-flex justify-content-between align-items-start gap-3">
                                    <div>
                                        <strong><%= HtmlUtil.escape(rating.getUsername()) %></strong>
                                        <div class="muted-small"><%= TimeUtil.format(rating.getUpdatedAt()) %></div>
                                    </div>
                                    <span class="badge bg-warning text-dark"><%= rating.getRating() %>/5</span>
                                </div>
                                <% if (rating.getComment() != null && !rating.getComment().isBlank()) { %>
                                    <p class="mt-2 mb-0"><%= HtmlUtil.escape(rating.getComment()) %></p>
                                <% } %>
                            </div>
                        <% } %>
                    </div>
                <% } %>
            </div>
        </div>
    </div>
    <div class="col-lg-4">
        <div class="card shadow-sm mb-4">
            <div class="card-body">
                <h2 class="h5">Attachments</h2>
                <% if (attachments == null || attachments.isEmpty()) { %>
                    <p class="text-muted mb-0">No attachments uploaded.</p>
                <% } else { %>
                    <ul class="mb-0 ps-3">
                        <% for (Attachment attachment : attachments) { %>
                            <li>
                                <a href="<%= request.getContextPath() %>/attachments/download?id=<%= attachment.getId() %><%= shareToken != null && !shareToken.isBlank() ? "&token=" + HtmlUtil.escape(shareToken) : "" %>"><%= HtmlUtil.escape(attachment.getOriginalFilename()) %></a>
                            </li>
                        <% } %>
                    </ul>
                <% } %>
            </div>
        </div>
        <% if (Boolean.TRUE.equals(isOwner)) { %>
            <div class="card shadow-sm">
                <div class="card-body">
                    <h2 class="h5">Share link</h2>
                    <% if (shareUrl != null) { %>
                        <div class="alert alert-light border">
                            <div class="small text-muted mb-1">Active link</div>
                            <a href="<%= HtmlUtil.escape(shareUrl) %>"><%= HtmlUtil.escape(shareUrl) %></a>
                        </div>
                        <form action="<%= request.getContextPath() %>/notes/share/revoke" method="post" class="mb-2">
                            <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                            <input type="hidden" name="noteId" value="<%= note.getId() %>">
                            <button class="btn btn-outline-danger w-100" type="submit">Revoke link</button>
                        </form>
                    <% } %>
                    <form action="<%= request.getContextPath() %>/notes/share/generate" method="post">
                        <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                        <input type="hidden" name="noteId" value="<%= note.getId() %>">
                        <button class="btn btn-outline-primary w-100" type="submit"><%= shareUrl == null ? "Generate share link" : "Regenerate share link" %></button>
                    </form>
                </div>
            </div>
        <% } %>
    </div>
</div>
<%@ include file="../layout/footer.jspf" %>
