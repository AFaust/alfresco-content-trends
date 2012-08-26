<#assign el=args.htmlid?html>
<div id="${el}-configDialog" class="config-content-trends-dialog">
    <div id="${el}-title" class="hd">${msg("label.configure-dashlet")?html}</div>
    <div class="bd">
        <form id="${el}-form" action="" method="post">
            <div class="yui-gd">
                <div class="yui-u first"><label for="${el}-usePagination">${msg("label.usePagination")?html}:</label></div>
                <div class="yui-u"><input id="${el}-usePagination" type="checkbox" name="usePagination" <#if pagination?? && pagination>checked="checked"</#if> /></div>
            </div>
            <div class="yui-gd">
                <div class="yui-u first"><label for="${el}-pageSize">${msg("label.pageSize")?html}:</label></div>
                <div class="yui-u">
                    <select id="${el}-pageSize" name="pageSize">
                        <#assign limits = [5, 10, 15, 25, 50] />
                        <#list limits as limit><option value="${limit}" <#if pageSize?? && pageSize == limit>selected="selected"</#if>>${msg("label.limit", limit)?html}</option></#list>
                    </select>
                </div>
            </div>
            <div class="bdft">
                <input type="button" id="${el}-ok" value="${msg("button.apply")}" tabindex="0" />
                <input type="button" id="${el}-cancel" value="${msg("button.cancel")}" tabindex="0" />
            </div>
        </form>
    </div>
</div>