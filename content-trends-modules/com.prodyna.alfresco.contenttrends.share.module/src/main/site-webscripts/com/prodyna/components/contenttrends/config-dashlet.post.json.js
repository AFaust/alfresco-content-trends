function main()
{
    var c = sitedata.getComponent(url.templateArgs.componentId);

    if (json.has("pageSize"))
    {
        var pageSize = json.get("pageSize");
        c.properties["pageSize"] = pageSize;
    }

    if (json.has("usePagination"))
    {
        var usePagination = json.get("usePagination");
        c.properties["pagination"] = usePagination;
    }
    
    c.save();
}

main();