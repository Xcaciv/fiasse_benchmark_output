<%@ page import="com.loosenotes.util.CsrfUtil,com.loosenotes.util.HtmlUtil" %>
<%@ include file="../layout/header.jspf" %>
<%
    Boolean validToken = (Boolean) request.getAttribute("validToken");
    String token = (String) request.getAttribute("token");
%>
<div class="row justify-content-center">
    <div class="col-md-7 col-lg-6">
        <div class="card shadow-sm">
            <div class="card-body">
                <h1 class="h4 mb-3">Choose a new password</h1>
                <% if (Boolean.TRUE.equals(validToken)) { %>
                    <form action="<%= request.getContextPath() %>/auth/reset-password" method="post" class="d-grid gap-3">
                        <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                        <input type="hidden" name="token" value="<%= HtmlUtil.escape(token == null ? "" : token) %>">
                        <div>
                            <label class="form-label" for="password">New password</label>
                            <input class="form-control" id="password" name="password" type="password" required>
                        </div>
                        <div>
                            <label class="form-label" for="confirmPassword">Confirm new password</label>
                            <input class="form-control" id="confirmPassword" name="confirmPassword" type="password" required>
                        </div>
                        <button class="btn btn-primary" type="submit">Update password</button>
                    </form>
                <% } else { %>
                    <div class="alert alert-danger mb-0">This reset link is invalid, expired, or already used.</div>
                <% } %>
            </div>
        </div>
    </div>
</div>
<%@ include file="../layout/footer.jspf" %>
