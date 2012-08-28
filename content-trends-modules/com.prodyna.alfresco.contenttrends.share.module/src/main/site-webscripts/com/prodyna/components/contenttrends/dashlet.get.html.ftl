<#function globalConfig key default>
   <#if config.global.flags??>
      <#assign values = config.global.flags.childrenMap[key]>
      <#if values?? && values?is_sequence>
         <#return values[0].value>
      </#if>
   </#if>
   <#return default>
</#function>

<#assign id = args.htmlid>
<#assign jsid = args.htmlid?js_string>

<script type="text/javascript">//<![CDATA[
(function()
{
    var dashlet = new PRODYNA.dashlet.ContentTrends("${jsid}").setOptions(
    {
        componentId: "${instance.object.id}",
        siteId: "${page.url.templateArgs.site!""}",
        <#-- TODO: proper containerId for site? -->
        <#--containerId: "${template.properties.container!"documentLibrary"}",-->
        repositoryBrowsing: ${(rootNode??)?string},
        rootNode: "${rootNode!"null"}",
        usePagination: ${(args.pagination!true)?string},
        pageSize: ${args.pageSize!10},
        sortField: "${(preferences.sortField!"ct:totalScore")?js_string}",
        useTitle: ${(useTitle!true)?string}
        
    }).setMessages(${messages});
    new Alfresco.widget.DashletResizer("${jsid}", "${instance.object.id}");
    
    var contentTrendsDashletEvent = new YAHOO.util.CustomEvent("onConfigContentTrendsClick");
    contentTrendsDashletEvent.subscribe(dashlet.onConfigContentTrendsClick, dashlet, true);
    
    new Alfresco.widget.DashletTitleBarActions("${jsid}").setOptions(
    {
        actions:
        [
        <#if userIsSiteManager>
            {
                cssClass: "edit",
                eventOnClick: contentTrendsDashletEvent,
                tooltip: "${msg("dashlet.edit.tooltip")?js_string}"
            },
        </#if>
            {
                cssClass: "help",
                bubbleOnClick:
                {
                    message: "${msg("dashlet.help")?js_string}"
                },
                tooltip: "${msg("dashlet.help.tooltip")?js_string}"
            }
        ]
    });
})();
//]]></script>

<div class="dashlet content-trends">
    <div class="title">${msg("header")}</div>
    <div id="${id}-body" class="body doclist no-check-bg" style="height: ${args.height!"50"}px;">
        <div id="${id}-main-template" class="hidden">
            <div>
            </div>
        </div>
        
        <div id="${id}-doclistBar" class="doclist-bar flat-button no-check-bg">
            <div id="${id}-paginator" class="paginator"></div>
            <div class="sort-field">
                <span id="${id}-sortField-button" class="yui-button yui-push-button">
                    <span class="first-child">
                        <button name="content-trends-sortField-button"></button>
                    </span>
                </span>
                <span class="separator">&nbsp;</span>
                <select id="${id}-sortField-menu">
                <#list sortOptions as sort>
                    <option value="${(sort.value!"")?html}" <#if sort.direction??>title="${sort.direction?string}"</#if>>${msg(sort.label)}</option>
                </#list>
                </select>
            </div>
            <#-- TODO: provide sort direction option? -->
        </div>
        
        <div id="${id}-documents" class="documents"></div>
        
        <div id="${id}-doclistBarBottom" class="yui-gc doclist-bar doclist-bar-bottom flat-button">
            <div id="${id}-paginatorBottom" class="paginator"></div>
        </div>
        
        <div style="display: none">

        <div id="${id}-moreActions">
            <div class="internal-show-more" title="onActionShowMore"><a href="#" class="show-more" title="${msg("actions.more")}"><span>${msg("actions.more")}</span></a></div>
            <div class="more-actions hidden"></div>
        </div>

    </div>
</div>
<#assign DEBUG=(globalConfig("client-debug", "false") = "true") />
<script type="text/javascript" src="${page.url.context}/res/components/content-trends/content-trends-actions${DEBUG?string("", "-min")}.js" ></script>
<script type="text/javascript" src="${page.url.context}/res/components/content-trends/content-trends-renderers${DEBUG?string("", "-min")}.js" ></script>