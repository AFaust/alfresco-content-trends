<#assign el=args.htmlid?html>
<script type="text/javascript">//<![CDATA[
    Alfresco.util.ComponentManager.get("${el}").setMessages(${messages});
//]]></script>
<div id="${el}-dialog" class="node-selection-dialog">
    <div id="${el}-title" class="hd"></div>
    <div class="bd">
        <form id="${el}-form" action="" method="post">
            <#assign selectionFieldId = el + "-nodes" />
            <#assign selectionControlId = selectionFieldId + "-cntrl" />
            <#include "/org/alfresco/components/form/controls/common/picker.inc.ftl" />
            <div class="yui-gd">
                <div class="yui-u first"><label for="${selectionControlId}">${msg("label.compareWith")?html}:</label></div>
                    <div class="yui-u">
                        <div id="${selectionControlId}" class="object-finder">
                            <div id="${selectionControlId}-currentValueDisplay" class="current-values"></div>
                            <input type="hidden" id="${selectionFieldId}" name="-" value="" />
                            <input type="hidden" id="${selectionControlId}-added" name="nodes_added" />
                            <input type="hidden" id="${selectionControlId}-removed" name="nodes_removed" />
                            <div id="${selectionControlId}-itemGroupActions" class="show-picker"></div>
    
                            <@renderPickerHTML selectionControlId />
                        </div>
                    </div>
                </div>
            <div class="bdft">
                <input type="button" id="${el}-ok" value="${msg("button.apply")}" tabindex="0" />
                <input type="button" id="${el}-cancel" value="${msg("button.cancel")}" tabindex="0" />
            </div>
        </form>
    </div>
</div>