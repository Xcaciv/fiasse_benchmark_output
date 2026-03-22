<%@ page import="com.loosenotes.util.CsrfUtil,com.loosenotes.util.HtmlUtil" %>
<%@ include file="../layout/header.jspf" %>
<%
    String usernameValue = (String) request.getAttribute("usernameValue");
    String emailValue = (String) request.getAttribute("emailValue");
%>
<div class="row justify-content-center">
    <div class="col-md-7 col-lg-6">
        <div class="card shadow-sm">
            <div class="card-body">
                <h1 class="h4 mb-3">Create your account</h1>
                <form action="<%= request.getContextPath() %>/auth/register" method="post" class="d-grid gap-3">
                    <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                    <div>
                        <label class="form-label" for="username">Username</label>
                        <input class="form-control" id="username" name="username" value="<%= HtmlUtil.escape(usernameValue == null ? "" : usernameValue) %>" required>
                    </div>
                    <div>
                        <label class="form-label" for="email">Email</label>
                        <input class="form-control" id="email" name="email" type="email" value="<%= HtmlUtil.escape(emailValue == null ? "" : emailValue) %>" required>
                    </div>
                    <div>
                        <label class="form-label" for="password">Password</label>
                        <input class="form-control" id="password" name="password" type="password" required>
                    </div>
                    <div>
                        <label class="form-label" for="confirmPassword">Confirm password</label>
                        <input class="form-control" id="confirmPassword" name="confirmPassword" type="password" required>
                    </div>
                    <button class="btn btn-primary" type="submit">Register</button>
                </form>
            </div>
        </div>
    </div>
</div>
<%@ include file="../layout/footer.jspf" %>
