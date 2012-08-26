<#include "/org/alfresco/components/component.head.inc">
<#include "/org/alfresco/components/form/form.get.head.ftl">
<#include "/org/alfresco/components/documentlibrary/actions-common.get.head.ftl">

<!-- Document List for some common functionality -->
<@link rel="stylesheet" type="text/css" href="${page.url.context}/res/components/documentlibrary/documentlist.css" />
<@script type="text/javascript" src="${page.url.context}/res/modules/documentlibrary/doclib-actions.js" />
<@script type="text/javascript" src="${page.url.context}/res/components/documentlibrary/documentlist.js" />

<!-- Content Trends -->
<@link rel="stylesheet" type="text/css" href="${page.url.context}/res/components/content-trends/content-trends.css" />
<@script type="text/javascript" src="${page.url.context}/res/components/content-trends/content-trends.js" />

<#--
Needs to be included in HTML after instantiation for action to get picked up (similar to doclib-custom.get.html.ftl)
<@script type="text/javascript" src="${page.url.context}/res/components/content-trends/content-trends-actions.js" />
<@script type="text/javascript" src="${page.url.context}/res/components/content-trends/content-trends-renderers.js" />
-->
<@link rel="stylesheet" type="text/css" href="${page.url.context}/res/components/content-trends/content-trends-renderers.css" />

<!-- Dialogs -->
<@script type="text/javascript" src="${page.url.context}/res/modules/content-trends/node-selection-dialog.js" />
<#--
Not necessary as-of-yet
<@link rel="stylesheet" type="text/css" href="${page.url.context}/res/modules/content-trends/node-selection-dialog.css" />
-->

<@script type="text/javascript" src="${page.url.context}/res/modules/content-trends/node-trend-dialog.js" />
<@link rel="stylesheet" type="text/css" href="${page.url.context}/res/modules/content-trends/node-trend-dialog.css" />

<@script type="text/javascript" src="${page.url.context}/res/modules/content-trends/compare-trend-dialog.js" />
<@link rel="stylesheet" type="text/css" href="${page.url.context}/res/modules/content-trends/compare-trend-dialog.css" />

<!-- Required resources -->
<@script type="text/javascript" src="${page.url.context}/res/amcharts/amcharts.js" />
<@link rel="stylesheet" type="text/css" href="${page.url.context}/res/amcharts/amcharts.css" />