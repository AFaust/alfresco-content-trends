<#assign el=args.htmlid?html>
<script type="text/javascript">//<![CDATA[
    Alfresco.util.ComponentManager.get("${el}").setMessages(${messages});
//]]></script>
<div id="${el}-dialog" class="content-trends-dialog">
   <div id="${el}-title" class="hd"></div>
   <div class="bd">
      <form id="${el}-form" action="" method="post">
         <div id="${el}-chart" class="chart">
         </div>
         <div class="bdft">
            <input type="button" id="${el}-ok" value="${msg("button.apply")}" tabindex="0" />
            <input type="button" id="${el}-cancel" value="${msg("button.cancel")}" tabindex="0" />
         </div>
      </form>
   </div>
</div>