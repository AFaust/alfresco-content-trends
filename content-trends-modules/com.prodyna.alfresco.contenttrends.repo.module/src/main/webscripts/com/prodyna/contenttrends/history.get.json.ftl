<#escape x as jsonUtils.encodeJSONString(x)>
{
    "nodeRef" : "${node.nodeRef}",
    "name" : "${node.name}",
    "historicScores" : [
        <#list scoreHistory as historyEntry>
        {
            "date": "${xmldate(historyEntry.date)}",
            <#list scoreKeys as key>
            "${key}": "${historyEntry[key]}"<#if key_has_next>,</#if>
            </#list>
        }<#if historyEntry_has_next>,</#if>
        </#list>
    ]
}
</#escape>