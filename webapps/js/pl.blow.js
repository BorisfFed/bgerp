/*
 * Plugin Blow
 */
"use strict";

bgerp.blow = new function() {
	const debug = bgerp.debug("blow");
	
	const ATTR_BG_ID = "bg-id";
	const ATTR_BG_PARENT_ID = "bg-parent-id";
	const ATTR_BG_TYPE_ID = "bg-type-id";
	
	const CLASS_BORDER_TOP = "group-border-t";
	const CLASS_BORDER_LEFT = "group-border-l";
	const CLASS_BORDER_BOTTOM = "group-border-b";
	const CLASS_BORDER_RIGHT = "group-border-r";
	
	const CLASS_SELECTED = "selected";
	const CLASS_SELECTED_FILTER = "selected";
	
	const selectItem = ($td, $cells) => {
		const itemId = $td.attr(ATTR_BG_ID);
		const parentId = $td.attr(ATTR_BG_PARENT_ID);
		
		if (itemId) {
			$td.addClass(CLASS_SELECTED);
			$cells.filter("[" + ATTR_BG_PARENT_ID + "=" + itemId + "]").each(function () {
				selectItem($(this), $cells);
			});
		}
	};
		
	const initTable = ($table, $menu) => {
		const $cells = $table.find("> tbody > tr:gt(0) > td"); 
		
		$cells.each(function () {
			const $td = $(this);
			
			$td.mouseover(function () {
				debug("mouseover", $td);
				selectItem($td, $cells);
			});
			
			$td.mouseleave(function () {
				debug("mouseleave", $td);
				$cells.removeClass(CLASS_SELECTED);
			});
			
			if ($menu)
				bgerp.blow.drag.init($td);
			
			groupBorder($cells, $td);
		});
		
		if ($menu)
			initRcMenu($table, $menu);
	};
	
	const initRcMenu = ($table, $menu) => {
		const menu = $menu.menu();
		
		$table.on("contextmenu", "td", function (e) {
	        debug("contextmenu", e);	        
	        const $td = $(e.target);
	        const typeId = $td.attr(ATTR_BG_TYPE_ID);
	        if (typeId) {
		        const parentId = $td.attr(ATTR_BG_PARENT_ID);
		        
	        	menu.show().position({
					my: "left top",
					at: "left bottom",
					of: e
				});
	        	
	        	$(document).one("click", () => {
					menu.hide();
				});
				
				$menu.find("#create").click((e) => {
					let url = null;
					if (parentId > 0)
						url = "/user/process.do?action=linkProcessCreate&objectType=processMade&typeId=" + typeId + "&id=" + parentId;
					else
						url = "/user/process.do?action=processCreate&typeId=" + typeId;
					
					bgerp.ajax
						.post(url)
						.done((resp) => {
							bgerp.process.open(resp.data.process.id);
						});
				});
	        }
	        return false;
	    });
	};
	
	const groupBorder = ($cells, $td) => {
		const itemId = $td.attr(ATTR_BG_ID);
		const $children = $cells.filter("[" + ATTR_BG_PARENT_ID + "=" + itemId + "]");
		
		if (itemId == 0) 
			$td.addClass(CLASS_BORDER_TOP);
		else if (itemId > 0) {
			// group of cells
			if ($children.length > 0) {
				const $root = $td;
				
				debug($root, $children);
				
				$.merge($root, $children).each((i, td) => {
					$td = $(td);
					
					const $tr = $td.closest("tr");
					
					if ($tr.is(":nth-child(2)") || $td[0] === $root[0])
						$td.addClass(CLASS_BORDER_TOP);
					
					if ($tr.is(":last-child"))
						$td.addClass(CLASS_BORDER_BOTTOM);
									
					if ($td.is(":first-child") || $td.prev().attr(ATTR_BG_PARENT_ID) != itemId)
						$td.addClass(CLASS_BORDER_LEFT);
					
					if ($td.is(":last-child") || $td.next().attr(ATTR_BG_PARENT_ID) != itemId)
						$td.addClass(CLASS_BORDER_RIGHT);
				});
			}
			// standalone cell
			else if ($td.attr(ATTR_BG_PARENT_ID) == 0) {
				const $tr = $td.closest("tr");
				if ($tr.index() > 1 && getCellInSameColumn($tr.prev(), $td).attr(ATTR_BG_PARENT_ID) > 0)
					$td.addClass(CLASS_BORDER_TOP);				
			}
		} 
		// empty cell
		else if (!itemId) {
			if (!$td.is(":first-child") && isGroupMember($td.prev()))
				$td.addClass(CLASS_BORDER_LEFT);
			
			if (!$td.is(":last-child") && isGroupMember($td.next()))
				$td.addClass(CLASS_BORDER_RIGHT);
			
			const $tr = $td.closest("tr");
			
			if (!$tr.is(":nth-child(2)") && neighborCellNeedBorder($tr.prev(), $td))
				$td.addClass(CLASS_BORDER_TOP);
			
			if (!$tr.is(":last-child") && neighborCellNeedBorder($tr.next(), $td))
				$td.addClass(CLASS_BORDER_BOTTOM);
		}
	};
	
	const neighborCellNeedBorder = ($tr, $td) => {
		return isGroupMember(getCellInSameColumn($tr, $td));
	};
	
	const getCellInSameColumn = ($tr, $td) => {
		const index = $td.index();
		const $children = $tr.children();
		debug("getCellInSameColumn", $tr, $children);
		return  $children.length === 1 ?  $children.first() : $($children.get(index));
	};	
	
	const isGroupMember = ($td) => {
		return $td.attr(ATTR_BG_PARENT_ID) === "" || $td.attr(ATTR_BG_PARENT_ID) > 0;
	};
	
	const toggleFilterHighlight = ($table, $button) => {
		const $cells = $table.find("td.filter-" + $button.attr(ATTR_BG_ID));
		if ($button.toggleClass(CLASS_SELECTED_FILTER).hasClass(CLASS_SELECTED_FILTER))
			$cells.css("background-color", $button.css("color"));
		else
			$cells.css("background-color", "");
	};
	
	// drag & drop
	this.drag = new function () {
		let $cells = null; 
		
		const dragStart = function (e) {
			this.style.opacity = '0.4';			  
			e.originalEvent.dataTransfer.setData("text", $(this).attr(ATTR_BG_ID));
			$cells = $(this).closest("table").find("td");
		};
	
		const dragEnd = function (e) {
			this.style.opacity = '';
			$cells.removeClass(CLASS_SELECTED);
		};
	
		const dragOver = (e) => {
			debug("drag over", e);
			
			$cells.removeClass(CLASS_SELECTED);
			
			const $td = $(e.target);
			if ($td.prop("tagName") === "TD") {			
				const targetProcessId = $td.attr(ATTR_BG_ID);
				const targetParentProcessId = $td.attr(ATTR_BG_PARENT_ID);
				
				let $root = null;
				if (targetParentProcessId > 0)
					$root = $cells.filter("td[" + ATTR_BG_ID + "=" + targetParentProcessId + "]");
				else if (targetParentProcessId === "")
					$root = $td;
				
				if ($root)
					selectItem($root, $cells);
			}			
			e.preventDefault();
		}
	
		const dragDrop = function (e) {
			debug("drag drop", e);
			
			e.preventDefault();
			
			const processId = e.originalEvent.dataTransfer.getData("text");
			const $td = $(e.target).closest("table").find("td[" + ATTR_BG_ID + "=" + processId + "]");
			const targetProcessId = $(this).attr(ATTR_BG_ID);
			const targetParentProcessId = $(this).attr(ATTR_BG_PARENT_ID);
			
			bgerp.ajax
				.post("/user/plugin/blow/board.do?action=move&processId=" + processId + "&fromParentProcessId=" + $td.attr(ATTR_BG_PARENT_ID) + 
						"&parentProcessId=" + (targetParentProcessId > 0 ? targetParentProcessId : targetProcessId))
				.done(() => bgerp.shell.contentLoad("/user/blow/board"));
			
			return false;
		}
		
		const initDD = ($td) => {
			$td.on('dragstart', dragStart);
			$td.on('dragend', dragEnd);
			$td.on('dragover', dragOver);
			$td.on('drop', dragDrop);
		};
		
		this.init = initDD;
	}
	
	// public functions
	this.initTable = initTable;
	this.toggleFilterHighlight = toggleFilterHighlight;
};


