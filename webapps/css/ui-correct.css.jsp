<%@ page contentType="text/html; charset=UTF-8"%>

.ui-tabs { padding: 0;}
.ui-tabs .ui-tabs-panel { padding: 0; }
.ui-helper-reset { font-size: 1em; line-height: 1; }
.ui-tabs .ui-tabs-nav li a { float: left; padding: 0.3em 0.3em; text-decoration: none; }

.ui-button-text-only .ui-button-text .ui-button-icon-only { padding: 0.2em 1em; }
.ui-button .ui-button-text { line-height: 1; }

.ui-widget, .ui-widget input, .ui-widget select
{
	font-size: 1em;
	font-family: Arial, sans-serif;
}
.ui-widget-content { color: black; }

/* menu icons */
.ui-icon {
	margin-top: 0;
}

.ui-autocomplete, .ui-menu, .ui-menu a,
.ui-tabs.ui-corner-all, .ui-tabs .ui-corner-all, .ui-tabs .ui-corner-top {
	border-bottom-right-radius: 0;
	border-bottom-left-radius: 0;
	border-top-right-radius: 0;
	border-top-left-radius: 0;
	text-decoration: none;
}

.ui-widget input, .ui-widget select, .ui-widget textarea, .ui-widget button {
	font-family: inherit;
}

.ui-datepicker {
	z-index: 1003 !important; /* must be > than popup editor (1002) */
}

.ui-datepicker-calendar .ui-state-default,
.ui-datepicker-calendar .ui-widget-content .ui-state-default, .ui-widget-header .ui-state-default {
	background: none;
	background-color: ${UI_TABLE_HEAD_BACKGROUND_COLOR};
	color: #448DAE;
}

.ui-widget-header .ui-state-default {
	background: none;
}