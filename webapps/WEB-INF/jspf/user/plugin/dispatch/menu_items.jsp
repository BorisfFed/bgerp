<%@ page contentType="text/html; charset=UTF-8"%>
<%@ include file="/WEB-INF/jspf/taglibs.jsp"%>

<ui:menu-group ltitle="Рассылки">
	<jsp:attribute name="subitems">
		<ui:menu-item ltitle="Рассылки" href="dispatch"
			action="ru.bgcrm.plugin.dispatch.struts.action.DispatchAction:dispatchList"
			command="/user/plugin/dispatch/dispatch.do?action=dispatchList" />

		<ui:menu-item ltitle="Сообщения рассылок" href="dispatch/message"
			action="ru.bgcrm.plugin.dispatch.struts.action.DispatchAction:messageList"
			command="/user/plugin/dispatch/dispatch.do?action=messageList" />
	</jsp:attribute>
</ui:menu-group>