<%@ page contentType="text/html; charset=UTF-8"%>
<%@ include file="/WEB-INF/jspf/taglibs.jsp"%>

<c:set var="board" scope="request" value="${form.response.data.board}"/>
<c:if test="${not empty board}">
	<c:set var="uiid" value="${u:uiid()}"/>	

    <%@ include file="filters.jsp"%>	
    	
	<table id="${uiid}" class="data" style="width: 100%;">
		<tr class="head">
			<c:forEach var="column" items="${board.executors}">
				<td>${column.title}<%@ include file="executor_count.jsp"%></td>
			</c:forEach>
		</tr>
		
		<%@ include file="table_data.jsp"%>	
	</table>
	
	<c:set var="uiidRcMenu" value="${u:uiid()}"/>
	<ul style="display: none; z-index: 2000;" id="${uiidRcMenu}">
		<li id="create"><a>${l.l('Новый процесс')}</a></li>
	</ul>
	
	<script>
	$(function () {
		bgerp.blow.initTable($('#${uiid}'), $('#${uiidRcMenu}'));
		
	    $('#content > #blow-board').data('onShow', function () {
	    	openUrlContent("/user/plugin/blow/board.do?action=show&id=" + ${form.id});
	    });
	})
	</script>
</c:if>
