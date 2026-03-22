<%@ page import="com.loosenotes.util.CsrfUtil,com.loosenotes.util.HtmlUtil" %>
<%@ include file="../layout/header.jspf" %>
<%
    String identifier = (String) request.getAttribute("identifierValue");
%>
<div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
        <div class="card shadow-sm">
            <div class="card-body">
                <h1 class="h4 mb-3">Sign in</h1>
                <form action="<%= request.getContextPath() %>/auth/login" method="post" class="d-grid gap-3">
                    <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                    <div>
                        <label class="form-label" for="identifier">Username or email</label>
                        <input class="form-control" id="identifier" name="identifier" value="<%= HtmlUtil.escape(identifier == null ? "" : identifier) %>" required>
                    </div>
                    <div>
                        <label class="form-label" for="password">Password</label>
                        <input class="form-control" id="password" name="password" type="password" required>
                    </div>
                    <button class="btn btn-primary" type="submit">Sign in</button>
                </form>
                <div class="mt-3 d-flex justify-content-between">
                    <a href="<%= request.getContextPath() %>/auth/register">Create account</a>
                    <a href="<%= request.getContextPath() %>/auth/forgot-password">Forgot password?</a>
                </div>
            </div>
        </div>
    </div>
</div>
<%@ include file="../layout/footer.jspf" %>
