<#assign el=args.htmlid?html>
<script type="text/javascript">//<![CDATA[
    Alfresco.util.ComponentManager.get("${el}").setMessages(${messages});
//]]></script>
<div id="${el}-dialog" class="compare-trends-dialog">
    <div id="${el}-title" class="hd"></div>
    <div class="bd">
        <form id="${el}-form" action="" method="post">
            <div class="yui-gd">
                <div class="yui-u first"><label for="${el}-scoreType">${msg("label.scoreType")?html}:</label></div>
                <div class="yui-u">
                    <select id="${el}-scoreType" name="scoreType" tabindex="0">
                        <option value="totalScore" selected="selected">${msg("node-trend.total-score")?html}</option>
                        <option value="viewScore">${msg("node-trend.view-score")?html}</option>
                        <option value="editScore">${msg("node-trend.edit-score")?html}</option>
                        <option value="tagScore">${msg("node-trend.tag-score")?html}</option>
                        <option value="ratingScore">${msg("node-trend.rating-score")?html}</option>
                        <option value="commentScore">${msg("node-trend.comment-score")?html}</option>
                    </select>
                </div>
            </div>
            <div id="${el}-chart" class="chart"></div>
            <div class="bdft">
                <input type="button" id="${el}-ok" value="${msg("button.apply")}" tabindex="0" />
                <input type="button" id="${el}-cancel" value="${msg("button.cancel")}" tabindex="0" />
            </div>
        </form>
    </div>
</div>