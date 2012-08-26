<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary-v2/parse-args.lib.js">

function main()
{
    // Use helper function to get the arguments
    var parsedArgs = ParseArgs.getParsedArgs();
    if (parsedArgs === null)
    {
        return;
    }
    
    var node = parsedArgs.rootNode;
    
    model.scoreHistory = contentTrends.getScoreHistory(node);
    model.scoreKeys = contentTrends.getScoreKeys();
    model.node = node;
}

main();