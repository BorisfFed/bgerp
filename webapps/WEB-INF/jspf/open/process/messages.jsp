<%@ page contentType="text/html; charset=UTF-8"%>
<%@ include file="/WEB-INF/jspf/taglibs.jsp"%>

<h2>${l.l('Сообщения')}</h2>

<%--  required for recognition of messages, belong to the process --%>
${form.setParam('processId', form.id.toString())}
<%@ include file="/WEB-INF/jspf/user/message/process_message_list.jsp"%>
