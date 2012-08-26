<import resource="classpath:/alfresco/site-webscripts/org/alfresco/components/documentlibrary/include/documentlist.lib.js">

(function()
{
    model.rootNode = DocumentList.getConfigValue("RepositoryLibrary", "root-node", "alfresco://company/home");
    model.sortOptions = DocumentList.getSortOptions();
    model.useTitle = DocumentList.getConfigValue("DocumentLibrary", "use-title", null);

    var PREFERENCES_KEY = "com.prodyna.share.content-trends." + instance.object.id, preferences = {};

    try
    {
        // Request the current user's preferences
        var result = remote.call("/api/people/" + encodeURIComponent(user.name) + "/preferences");
        if (result.status == 200 && result != "{}")
        {
            var prefs = eval('(' + result + ')');
            // Populate the preferences object literal for easy look-up later
            preferences = eval('(prefs.' + PREFERENCES_KEY + ')');
            if (typeof preferences != "object")
            {
                preferences = {};
            }
        }
    }
    catch (e)
    {
        // DO NOTHING
    }

    model.preferences = preferences;

    model.sortOptions = [
    {
        value : "ct:totalScore",
        direction : null,
        label : "node-trend.total-score"
    },
    {
        value : "ct:viewScore",
        direction : null,
        label : "node-trend.view-score"
    },
    {
        value : "ct:editScore",
        direction : null,
        label : "node-trend.edit-score"
    },
    {
        value : "ct:tagScore",
        direction : null,
        label : "node-trend.tag-score"
    },
    {
        value : "ct:ratingScore",
        direction : null,
        label : "node-trend.rating-score"
    },
    {
        value : "ct:commentScore",
        direction : null,
        label : "node-trend.comment-score"
    }];

    var userIsSiteManager = true;
    if (page.url.templateArgs.site)
    {
        // We are in the context of a site, so call the repository to see if the user is site manager or not
        userIsSiteManager = false;
        var json = remote.call("/api/sites/" + page.url.templateArgs.site + "/memberships/" + encodeURIComponent(user.name));

        if (json.status == 200)
        {
            var obj = eval('(' + json + ')');
            if (obj)
            {
                userIsSiteManager = (obj.role == "SiteManager");
            }
        }
    }
    model.userIsSiteManager = userIsSiteManager;
})();