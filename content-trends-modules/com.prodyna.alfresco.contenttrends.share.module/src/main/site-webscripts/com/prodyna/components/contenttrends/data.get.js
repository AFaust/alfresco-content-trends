<import resource="classpath:/alfresco/site-webscripts/org/alfresco/components/documentlibrary/data/surf-doclist.lib.js">

// override the various resolvers (e.g. action group and data webscript)
(function()
{
    var doclistActionGroupResolver = null;
    var doclistDataUrlResolver = null;

    DocList_Custom.calculateActionGroupId = function(item, view, itemJSON)
    {
        // Default group calculation
        if (!doclistActionGroupResolver)
        {
            doclistActionGroupResolver = resolverHelper.getDoclistActionGroupResolver("resolver.content-trends.actionGroup");
        }
        return (doclistActionGroupResolver.resolve(itemJSON, view) + "");
    };

})();

surfDoclist_main();