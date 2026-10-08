<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ include file="../layout/header.jspf" %>
<%@ include file="../layout/notifications.jspf" %>
<section class="card error-panel" aria-labelledby="error-title">
    <h1 id="error-title">Không thể hoàn tất yêu cầu</h1>
    <p><c:out value="${error.message}" default="Vui lòng thử lại sau."/></p>
    <c:if test="${not empty error.correlationId}">
        <p class="muted">Mã tra cứu: <c:out value="${error.correlationId}"/></p>
    </c:if>
    <a class="button" href="<c:url value='/events'/>">Về danh sách sự kiện</a>
</section>
<%@ include file="../layout/footer.jspf" %>
