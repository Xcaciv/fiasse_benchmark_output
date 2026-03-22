<%@ page import="com.loosenotes.util.HtmlUtil" isErrorPage="true" %>
<%@ include file="layout/header.jspf" %>
<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card shadow-sm border-danger">
            <div class="card-body">
                <h1 class="h3 text-danger">Something went wrong</h1>
                <p>The application hit an unexpected error while processing your request.</p>
                <% if (exception != null) { %>
                    <div class="alert alert-light border mb-0">
                        <strong>Error:</strong> <%= HtmlUtil.escape(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage()) %>
                    </div>
                <% } %>
            </div>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jspf" %>
