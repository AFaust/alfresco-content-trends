<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary-v2/evaluator.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/com/prodyna/contenttrends/filters.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary-v2/parse-args.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary-v2/doclist.lib.js">

// override getLocation to handle site content properly even if libraryRoot is present
Common.defaultGetLocation = Common.getLocation;
Common.getLocation = function(node, libraryRoot)
{
    var qnamePaths = node.qnamePath.split("/");

    if (qnamePaths[2] == TYPE_SITES)
    {
        libraryRoot = null;
    }

    return Common.defaultGetLocation(node, libraryRoot);
};

/**
 * Document List Component: doclist
 */
model.doclist = doclist_main();