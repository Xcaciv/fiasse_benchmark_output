<%@ page import="java.util.List,com.loosenotes.model.Note,com.loosenotes.model.Attachment,com.loosenotes.util.CsrfUtil,com.loosenotes.util.HtmlUtil,com.loosenotes.util.ValidationUtil" %>
<%@ include file="../layout/header.jspf" %>
<%
    Note note = (Note) request.getAttribute("note");
    Boolean editing = (Boolean) request.getAttribute("editing");
    List<Attachment> attachments = (List<Attachment>) request.getAttribute("attachments");
    String titleValue = (String) request.getAttribute("titleValue");
    String contentValue = (String) request.getAttribute("contentValue");
    Boolean isPublicValue = (Boolean) request.getAttribute("isPublicValue");
    String title = titleValue != null ? titleValue : (note == null ? "" : note.getTitle());
    String content = contentValue != null ? contentValue : (note == null ? "" : note.getContent());
    boolean isPublic = isPublicValue != null ? isPublicValue : (note != null && note.isPublic());
    boolean isEditing = Boolean.TRUE.equals(editing);
    String action = isEditing ? request.getContextPath() + "/notes/edit" : request.getContextPath() + "/notes/create";
%>
<div class="row justify-content-center">
    <div class="col-lg-9">
        <div class="card shadow-sm">
            <div class="card-body">
                <h1 class="h3 mb-3"><%= isEditing ? "Edit Note" : "Create Note" %></h1>
                <form action="<%= action %>" method="post" enctype="multipart/form-data" class="d-grid gap-3">
                    <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                    <% if (isEditing) { %>
                        <input type="hidden" name="id" value="<%= note.getId() %>">
                    <% } %>
                    <div>
                        <label class="form-label" for="title">Title</label>
                        <input class="form-control" id="title" name="title" value="<%= HtmlUtil.escape(title) %>" required>
                    </div>
                    <div>
                        <label class="form-label" for="content">Content</label>
                        <textarea class="form-control" id="content" name="content" rows="10" required><%= HtmlUtil.escape(content) %></textarea>
                    </div>
                    <div class="form-check form-switch">
                        <input class="form-check-input" type="checkbox" id="isPublic" name="isPublic" <%= isPublic ? "checked" : "" %>>
                        <label class="form-check-label" for="isPublic">Make this note public</label>
                    </div>
                    <div>
                        <label class="form-label" for="attachments">Attachments</label>
                        <input class="form-control" id="attachments" name="attachments" type="file" multiple>
                        <div class="form-text">Allowed: <%= HtmlUtil.escape(ValidationUtil.allowedExtensionsLabel()) %>. Max 5 MB per file.</div>
                    </div>
                    <% if (attachments != null && !attachments.isEmpty()) { %>
                        <div>
                            <h2 class="h6">Existing attachments</h2>
                            <ul class="mb-0">
                                <% for (Attachment attachment : attachments) { %>
                                    <li><a href="<%= request.getContextPath() %>/attachments/download?id=<%= attachment.getId() %>"><%= HtmlUtil.escape(attachment.getOriginalFilename()) %></a></li>
                                <% } %>
                            </ul>
                        </div>
                    <% } %>
                    <div class="d-flex gap-2">
                        <button class="btn btn-primary" type="submit"><%= isEditing ? "Save changes" : "Create note" %></button>
                        <a class="btn btn-outline-secondary" href="<%= request.getContextPath() %>/notes">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<%@ include file="../layout/footer.jspf" %>
