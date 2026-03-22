<%@ page import="java.util.List,com.loosenotes.model.NoteSummary,com.loosenotes.util.HtmlUtil,com.loosenotes.util.TimeUtil" %>
<%@ include file="layout/header.jspf" %>
<%
    List<NoteSummary> notes = (List<NoteSummary>) request.getAttribute("notes");
%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h1 class="h3 mb-0">Top Rated Notes</h1>
    <span class="text-muted">Minimum 3 ratings required</span>
</div>
<% if (notes == null || notes.isEmpty()) { %>
    <div class="alert alert-secondary">No public notes meet the minimum rating threshold yet.</div>
<% } else { %>
    <div class="row g-3">
        <% for (NoteSummary note : notes) { %>
            <div class="col-lg-6">
                <div class="card shadow-sm h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start gap-2">
                            <div>
                                <h2 class="h5 mb-1"><a class="text-decoration-none" href="<%= request.getContextPath() %>/notes/view?id=<%= note.getId() %>"><%= HtmlUtil.escape(note.getTitle()) %></a></h2>
                                <div class="muted-small">By <strong><%= HtmlUtil.escape(note.getAuthorUsername()) %></strong> · <%= TimeUtil.format(note.getCreatedAt()) %></div>
                            </div>
                            <span class="badge bg-warning text-dark"><%= String.format("%.2f", note.getAverageRating()) %></span>
                        </div>
                        <p class="mt-3"><%= HtmlUtil.escape(note.getExcerpt()) %></p>
                        <div class="muted-small"><%= note.getRatingCount() %> rating(s)</div>
                    </div>
                </div>
            </div>
        <% } %>
    </div>
<% } %>
<%@ include file="layout/footer.jspf" %>
