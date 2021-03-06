<%@ page contentType="text/html; charset=UTF-8"%>
<%@ include file="/WEB-INF/jspf/taglibs.jsp"%>

<%@ include file="search_common.jsp"%>

<c:set var="config" value="${u:getConfig(ctxSetup, 'ru.bgcrm.dao.message.config.MessageTypeConfig')}"/>

<table style="width: 100%;" class="data mt1">
	<tr>
		<td width="30">ID</td>
		<td>Тип</td>
		<td>Время</td>
		<td>Процесс</td>
	</tr>
	<c:forEach var="item" items="${form.response.data.list}">
		<c:set var="message" value="${item.first}"/>
		<c:set var="messageType" value="${config.typeMap[message.typeId]}"/>
		<c:set var="process" value="${item.second}"/>
		<tr>
			<td>${message.id}</td>
			<td>
				<%@ include file="/WEB-INF/jspf/user/message/message_direction.jsp"%>&nbsp;
				${messageType.title}
			</td>
			<td nowrap="nowrap">${u:formatDate(message.fromTime, 'ymdhm')}</td>
			<td>
				<c:if test="${not empty process}">
					<ui:process-link id="${process.id}" text="${process.description}"/>
				</c:if>
			</td>
		</tr>
	</c:forEach>
</table>