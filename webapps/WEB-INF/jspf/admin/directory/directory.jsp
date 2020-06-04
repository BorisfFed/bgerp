<%@ page contentType="text/html; charset=UTF-8"%>
<%@ include file="/WEB-INF/jspf/taglibs.jsp"%>

<c:set var="uiid" value="${u:uiid()}"/>

<form action="/admin/directory.do" id="${uiid}">
	<input type="hidden" name="action"/>
	
	<ui:combo-single hiddenName="directoryId" value="${form.param.directoryId}" 
		prefixText="${l.l('Справочник')}:" widthTextValue="200px"
		onSelect="
				this.form.action.value = $(item).attr('action'); 
				this.form.directoryId.value = $hidden.val();
				$$.ajax.load(this.form, $$.shell.$content())">
		<jsp:attribute name="valuesHtml">
			<c:forEach var="item" items="${directoryList}">
				<li value="${item.id}" action="${item.action}">${l.l(item.title)}</li>
			</c:forEach>
		</jsp:attribute>
	</ui:combo-single>
</form>

<script>
	$(function () {
		var $left = $('#title > .status:visible > .wrap > .left');
		$left.html("");
		$('#${uiid}').appendTo($left);
	})
</script>