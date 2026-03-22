<%@ page import="com.loosenotes.util.CsrfUtil,com.loosenotes.util.HtmlUtil" %>
<%@ include file="../layout/header.jspf" %>
<%
    String emailValue = (String) request.getAttribute("emailValue");
    String demoResetLink = (String) request.getAttribute("demoResetLink");
%>
<div class="row justify-content-center">
    <div class="col-md-7 col-lg-6">
        <div class="card shadow-sm">
            <div class="card-body">
                <h1 class="h4 mb-3">Reset your password</h1>
                <p class="text-muted">Enter your email address and we will generate a reset link that stays valid for one hour.</p>
                <form action="<%= request.getContextPath() %>/auth/forgot-password" method="post" class="d-grid gap-3">
                    <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                    <div>
                        <label class="form-label" for="email">Email address</label>
                        <input class="form-control" id="email" name="email" type="email" value="<%= HtmlUtil.escape(emailValue == null ? "" : emailValue) %>" required>
                    </div>
                    <button class="btn btn-primary" type="submit">Generate reset link</button>
                </form>
                <% if (demoResetLink != null) { %>
                    <div class="alert alert-warning mt-3 mb-0">
                        <strong>Demo mode:</strong> use this generated reset link locally:<br>
                        <a href="<%= HtmlUtil.escape(demoResetLink) %>"><%= HtmlUtil.escape(demoResetLink) %></a>
                    </div>
                <% } %>
            </div>
        </div>
    </div>
</div>
<%@ include file="../layout/footer.jspf" %>
