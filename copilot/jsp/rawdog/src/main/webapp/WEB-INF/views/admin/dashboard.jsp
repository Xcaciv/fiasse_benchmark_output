<%@ page import="java.util.List,com.loosenotes.model.AdminUserView,com.loosenotes.model.AdminNoteView,com.loosenotes.model.ActivityLog,com.loosenotes.model.User,com.loosenotes.util.CsrfUtil,com.loosenotes.util.HtmlUtil,com.loosenotes.util.TimeUtil" %>
<%@ include file="../layout/header.jspf" %>
<%
    long userCount = request.getAttribute("userCount") == null ? 0L : (Long) request.getAttribute("userCount");
    long noteCount = request.getAttribute("noteCount") == null ? 0L : (Long) request.getAttribute("noteCount");
    String userQuery = (String) request.getAttribute("userQuery");
    List<AdminUserView> users = (List<AdminUserView>) request.getAttribute("users");
    List<User> allUsers = (List<User>) request.getAttribute("allUsers");
    List<AdminNoteView> recentNotes = (List<AdminNoteView>) request.getAttribute("recentNotes");
    List<ActivityLog> recentActivity = (List<ActivityLog>) request.getAttribute("recentActivity");
%>
<div class="row g-4 mb-4">
    <div class="col-md-6 col-xl-3">
        <div class="card shadow-sm"><div class="card-body"><div class="text-muted">Total users</div><div class="display-6"><%= userCount %></div></div></div>
    </div>
    <div class="col-md-6 col-xl-3">
        <div class="card shadow-sm"><div class="card-body"><div class="text-muted">Total notes</div><div class="display-6"><%= noteCount %></div></div></div>
    </div>
</div>
<div class="row g-4">
    <div class="col-xl-7">
        <div class="card shadow-sm mb-4">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h1 class="h4 mb-0">Users</h1>
                </div>
                <form action="<%= request.getContextPath() %>/admin" method="get" class="row g-2 mb-3">
                    <div class="col-sm-9">
                        <input class="form-control" name="userQuery" value="<%= HtmlUtil.escape(userQuery == null ? "" : userQuery) %>" placeholder="Search by username or email">
                    </div>
                    <div class="col-sm-3 d-grid">
                        <button class="btn btn-outline-primary" type="submit">Search</button>
                    </div>
                </form>
                <div class="table-responsive">
                    <table class="table table-sm align-middle">
                        <thead>
                            <tr><th>User</th><th>Role</th><th>Registered</th><th>Notes</th></tr>
                        </thead>
                        <tbody>
                            <% for (AdminUserView user : users) { %>
                                <tr>
                                    <td>
                                        <strong><%= HtmlUtil.escape(user.getUsername()) %></strong><br>
                                        <span class="muted-small"><%= HtmlUtil.escape(user.getEmail()) %></span>
                                    </td>
                                    <td><span class="badge <%= "ADMIN".equals(user.getRole()) ? "bg-danger" : "bg-secondary" %>"><%= HtmlUtil.escape(user.getRole()) %></span></td>
                                    <td><%= TimeUtil.format(user.getCreatedAt()) %></td>
                                    <td><%= user.getNoteCount() %></td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <div class="card shadow-sm">
            <div class="card-body">
                <h2 class="h4">Recent notes / reassign ownership</h2>
                <div class="table-responsive">
                    <table class="table table-sm align-middle">
                        <thead>
                            <tr><th>Note</th><th>Owner</th><th>Visibility</th><th>Reassign</th></tr>
                        </thead>
                        <tbody>
                            <% for (AdminNoteView note : recentNotes) { %>
                                <tr>
                                    <td>
                                        <a href="<%= request.getContextPath() %>/notes/view?id=<%= note.getNoteId() %>"><%= HtmlUtil.escape(note.getTitle()) %></a><br>
                                        <span class="muted-small">Created <%= TimeUtil.format(note.getCreatedAt()) %> · #<%= note.getNoteId() %></span>
                                    </td>
                                    <td><%= HtmlUtil.escape(note.getOwnerUsername()) %></td>
                                    <td><span class="badge <%= note.isPublic() ? "bg-success" : "bg-secondary" %>"><%= note.isPublic() ? "Public" : "Private" %></span></td>
                                    <td>
                                        <form action="<%= request.getContextPath() %>/admin/reassign" method="post" class="d-flex gap-2">
                                            <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                                            <input type="hidden" name="noteId" value="<%= note.getNoteId() %>">
                                            <select class="form-select form-select-sm" name="userId">
                                                <% for (User candidate : allUsers) { %>
                                                    <option value="<%= candidate.getId() %>" <%= candidate.getId() == note.getOwnerUserId() ? "selected" : "" %>><%= HtmlUtil.escape(candidate.getUsername()) %></option>
                                                <% } %>
                                            </select>
                                            <button class="btn btn-sm btn-outline-primary" type="submit">Save</button>
                                        </form>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
    <div class="col-xl-5">
        <div class="card shadow-sm">
            <div class="card-body">
                <h2 class="h4">Recent activity log</h2>
                <% if (recentActivity == null || recentActivity.isEmpty()) { %>
                    <div class="text-muted">No activity logged yet.</div>
                <% } else { %>
                    <div class="list-group list-group-flush">
                        <% for (ActivityLog log : recentActivity) { %>
                            <div class="list-group-item px-0">
                                <div class="d-flex justify-content-between gap-3">
                                    <div>
                                        <div><strong><%= HtmlUtil.escape(log.getAction()) %></strong></div>
                                        <div class="muted-small"><%= HtmlUtil.escape(log.getDetails()) %></div>
                                    </div>
                                    <div class="muted-small text-nowrap"><%= TimeUtil.format(log.getCreatedAt()) %></div>
                                </div>
                            </div>
                        <% } %>
                    </div>
                <% } %>
            </div>
        </div>
    </div>
</div>
<%@ include file="../layout/footer.jspf" %>
