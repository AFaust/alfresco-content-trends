var Filters =
{
    DEFAULT_SCORE : "totalScore",

    VALID_SCORES :
    {
        totalScore : true,
        viewScore : true,
        ratingScore : true,
        commentScore : true,
        editScore : true
    },

    /**
     * Types that we want to suppress from the resultset
     */
    IGNORED_TYPES : [ "sys:container", "fm:post" ],

    /**
     * Aspects that we want to suppress from the resultset
     */
    IGNORED_ASPECTS : [ "rn:rendition", "cm:workingCopy" ],

    /**
     * Create filter parameters based on input parameters
     * 
     * @method getFilterParams
     * @param filter
     *            {string} Required filter
     * @param parsedArgs
     *            {object} Parsed arguments object literal
     * @param optional
     *            {object} Optional arguments depending on filter type
     * @return {object} Object literal containing parameters to be used in Lucene search
     */
    getFilterParams : function Filter_getFilterParams(filter, parsedArgs, optional)
    {
        var filterParams =
        {
            query : 'PATH:"' + parsedArgs.pathNode.qnamePath + '/*"',
            limitResults : null,
            sort : [
            {
                column : "@cm:name",
                ascending : true
            } ],
            language : "fts-alfresco",
            templates : null,
            variablePath : true,
            ignoreTypes : []
        };

        // Sorting parameters specified?
        var sortAscending = args.sortAsc, sortField = args.sortField;

        if (sortAscending == "false")
        {
            filterParams.sort[0].ascending = false;
        }

        if (sortField !== null)
        {
            filterParams.sort[0].column = (sortField.indexOf(":") != -1 ? "@" : "") + sortField;
        }

        // Max returned results specified?
        var argMax = args.max;
        if ((argMax !== null) && !isNaN(argMax))
        {
            filterParams.limitResults = argMax;
        }

        var filterQuery = "";

        // Common types and aspects to filter from the UI - known subtypes of cm:content and cm:folder
        var filterQueryDefaults = ' AND NOT TYPE:"' + Filters.IGNORED_TYPES.join('" AND NOT TYPE:"') + '" AND NOT ASPECT:"'
                + Filters.IGNORED_ASPECTS.join('" AND NOT ASPECT:"') + '"';

        // our filter is specified by the type filter
        var scoreType;
        if (parsedArgs.type in Filters.VALID_SCORES)
        {
            scoreType = parsedArgs.type;
        }
        else
        {
            logger.warn("Content Trends Filter: Defaulting score type due to invalid provided score type: " + parsedArgs.type);
            scoreType = Filters.DEFAULT_SCORE;
        }

        filterQuery = 'PATH:"' + parsedArgs.pathNode.qnamePath + '//*"'
        filterQuery += ' AND ASPECT:"ct:scored" AND NOT ISNULL("ct\:' + scoreType + '")';
        filterParams.query = filterQuery + filterQueryDefaults;

        // default to the score as a sort field if not explicitly defined
        if (sortField === null)
        {
            filterParams.sort[0].column = '@ct:' + scoreType;
        }

        // default to descending if not explicitly defined
        if (sortAscending === null)
        {
            filterParams.sort[0].ascending = false;
        }

        return filterParams;
    }
};