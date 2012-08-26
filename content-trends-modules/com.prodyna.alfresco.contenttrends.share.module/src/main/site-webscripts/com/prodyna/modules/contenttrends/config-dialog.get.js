function main()
{
    var componentId = args.componentId;

    if (componentId)
    {
        var c = sitedata.getComponent(componentId);
        if (c)
        {
            var pagination = c.properties["pagination"], pageSize = c.properties["pageSize"];
            if ((!pagination && (typeof pagination == "undefined")) || pagination == null)
            {
                // default
                pagination = true;
            }

            if ((!pageSize && (typeof pageSize == "undefined")) || pageSize == null)
            {
                // default
                pageSize = 10;
            }
            
            model.pagination = pagination;
            model.pageSize = pageSize;
        }
    }
}

main();