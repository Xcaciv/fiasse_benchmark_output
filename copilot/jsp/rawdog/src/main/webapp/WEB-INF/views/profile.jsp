<%@ page import="com.loosenotes.model.User,com.loosenotes.util.CsrfUtil,com.loosenotes.util.HtmlUtil" %>
<%@ include file="layout/header.jspf" %>
<%
    User user = (User) session.getAttribute("authUser");
    String usernameValue = (String) request.getAttribute("usernameValue");
    String emailValue = (String) request.getAttribute("emailValue");
%>
<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card shadow-sm">
            <div class="card-body">
                <h1 class="h3 mb-3">Profile</h1>
                <form action="<%= request.getContextPath() %>/profile" method="post" class="d-grid gap-3">
                    <input type="hidden" name="csrfToken" value="<%= CsrfUtil.token(session) %>">
                    <div>
                        <label class="form-label" for="username">Username</label>
                        <input class="form-control" id="username" name="username" value="<%= HtmlUtil.escape(usernameValue == null ? user.getUsername() : usernameValue) %>" required>
                    </div>
                    <div>
                        <label class="form-label" for="email">Email</label>
                        <input class="form-control" id="email" name="email" type="email" value="<%= HtmlUtil.escape(emailValue == null ? user.getEmail() : emailValue) %>" required>
                    </div>
                    <hr>
                    <h2 class="h5 mb-0">Change password</h2>
                    <div>
                        <label class="form-label" for="currentPassword">Current password</label>
                        <input class="form-control" id="currentPassword" name="currentPassword" type="password">
                    </div>
                    <div>
                        <label class="form-label" for="newPassword">New password</label>
                        <input class="form-control" id="newPassword" name="newPassword" type="password">
                    </div>
                    <div>
                        <label class="form-label" for="confirmPassword">Confirm new password</label>
                        <input class="form-control" id="confirmPassword" name="confirmPassword" type="password">
                    </div>
                    <button class="btn btn-primary" type="submit">Save profile</button>
                </form>
            </div>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jspf" %>
