<%@ page import="java.util.List,com.loosenotes.model.NoteSummary,com.loosenotes.util.HtmlUtil,com.loosenotes.util.TimeUtil" %>
<%@ include file="../layout/header.jspf" %>
<%
    List<NoteSummary> notes = (List<NoteSummary>) request.getAttribute("notes");
%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h1 class="h3 mb-0">My Notes</h1>
    <a class="btn btn-primary" href="<%= request.getContextPath() %>/notes/create">Create note</a>
</div>
<% if (notes == null || notes.isEmpty()) { %>
    <div class="alert alert-secondary">You have not created any notes yet.</div>
<% } else { %>
    <div class="row g-3">
        <% for (NoteSummary note : notes) { %>
            <div class="col-lg-6">
                <div class="card shadow-sm">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start gap-2">
                            <div>
                                <h2 class="h5 mb-1"><a class="text-decoration-none" href="<%= request.getContextPath() %>/notes/view?id=<%= note.getId() %>"><%= HtmlUtil.escape(note.getTitle()) %></a></h2>
                                <div class="muted-small">Created <%= TimeUtil.format(note.getCreatedAt()) %> · Updated <%= TimeUtil.format(note.getUpdatedAt()) %></div>
                            </div>
                            <span class="badge <%= note.isPublic() ? "bg-success" : "bg-secondary" %>"><%= note.isPublic() ? "Public" : "Private" %></span>
                        </div>
                        <p class="mt-3 mb-3"><%= HtmlUtil.escape(note.getExcerpt()) %></p>
                        <div class="d-flex justify-content-between align-items-center muted-small">
                            <span><%= String.format("%.1f", note.getAverageRating()) %>/5 from <%= note.getRatingCount() %> rating(s)</span>
                            <a href="<%= request.getContextPath() %>/notes/edit?id=<%= note.getId() %>">Edit</a>
                        </div>
                    </div>
                </div>
            </div>
        <% } %>
    </div>
<% } %>
<%@ include file="../layout/footer.jspf" %>
