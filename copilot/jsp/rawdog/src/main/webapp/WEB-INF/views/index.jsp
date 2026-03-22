<%@ page import="java.util.List,com.loosenotes.model.NoteSummary,com.loosenotes.util.HtmlUtil,com.loosenotes.util.TimeUtil,com.loosenotes.model.User" %>
<%@ include file="layout/header.jspf" %>
<%
    String query = (String) request.getAttribute("query");
    List<NoteSummary> results = (List<NoteSummary>) request.getAttribute("results");
    User current = (User) session.getAttribute("authUser");
%>
<div class="row g-4">
    <div class="col-lg-4">
        <div class="card shadow-sm">
            <div class="card-body">
                <h1 class="h4">Search notes</h1>
                <p class="text-muted">Find your own notes plus public notes from other users by title or content.</p>
                <form action="<%= request.getContextPath() %>/" method="get" class="d-grid gap-3">
                    <div>
                        <label class="form-label" for="q">Keywords</label>
                        <input class="form-control" id="q" name="q" value="<%= HtmlUtil.escape(query == null ? "" : query) %>" placeholder="Try a topic, phrase, or author note content">
                    </div>
                    <button class="btn btn-primary" type="submit">Search</button>
                </form>
                <% if (current == null) { %>
                    <hr>
                    <p class="mb-2">Need private notes, ratings, and uploads?</p>
                    <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/auth/register">Create an account</a>
                <% } %>
            </div>
        </div>
    </div>
    <div class="col-lg-8">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h2 class="h4 mb-0">Results</h2>
            <span class="text-muted"><%= results == null ? 0 : results.size() %> notes</span>
        </div>
        <% if (results == null || results.isEmpty()) { %>
            <div class="alert alert-secondary">No notes matched your search.</div>
        <% } else { %>
            <div class="row g-3">
                <% for (NoteSummary note : results) { %>
                    <div class="col-md-6">
                        <div class="card shadow-sm note-card">
                            <div class="card-body d-flex flex-column">
                                <div class="d-flex justify-content-between align-items-start gap-2">
                                    <h3 class="h5 mb-1"><a class="text-decoration-none" href="<%= request.getContextPath() %>/notes/view?id=<%= note.getId() %>"><%= HtmlUtil.escape(note.getTitle()) %></a></h3>
                                    <span class="badge <%= note.isPublic() ? "bg-success" : "bg-secondary" %>"><%= note.isPublic() ? "Public" : "Private" %></span>
                                </div>
                                <p class="muted-small mb-2">By <strong><%= HtmlUtil.escape(note.getAuthorUsername()) %></strong> on <%= TimeUtil.format(note.getCreatedAt()) %></p>
                                <p class="flex-grow-1"><%= HtmlUtil.escape(note.getExcerpt()) %></p>
                                <div class="d-flex justify-content-between align-items-center muted-small">
                                    <span><%= String.format("%.1f", note.getAverageRating()) %>/5 from <%= note.getRatingCount() %> rating(s)</span>
                                    <a href="<%= request.getContextPath() %>/notes/view?id=<%= note.getId() %>">Open</a>
                                </div>
                            </div>
                        </div>
                    </div>
                <% } %>
            </div>
        <% } %>
    </div>
</div>
<%@ include file="layout/footer.jspf" %>
