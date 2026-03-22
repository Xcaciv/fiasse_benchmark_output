<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
    if (statusCode == null) statusCode = 500;
    pageContext.setAttribute("statusCode", statusCode);
%>
<c:set var="pageTitle" value="Error – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<div class="row justify-content-center">
  <div class="col-md-6 text-center">
    <h1 class="display-1 text-danger"><c:out value="${statusCode}"/></h1>
    <c:choose>
      <c:when test="${statusCode == 403}"><h3>Access Denied</h3></c:when>
      <c:when test="${statusCode == 404}"><h3>Page Not Found</h3></c:when>
      <c:otherwise><h3>An Error Occurred</h3></c:otherwise>
    </c:choose>
    <p class="text-muted">
      <%-- No exception details exposed to the client --%>
      Something went wrong. Please try again or return to the home page.
    </p>
    <a href="${pageContext.request.contextPath}/home" class="btn btn-primary mt-2">Go Home</a>
  </div>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
