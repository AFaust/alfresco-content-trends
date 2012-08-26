/**
 * 
 */
package com.prodyna.alfresco.contenttrends.share.module.resolver.doclib;

import org.alfresco.web.resolver.doclib.DoclistActionGroupResolver;
import org.json.simple.JSONObject;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ContentTrendsDoclistActionGroupResolver implements DoclistActionGroupResolver
{

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String resolve(final JSONObject jsonObject, final String view)
    {
        // simple hardcoded group for now, as we only provide a single view at the moment
        final String actionGroupId = "content-trends-browse";
        return actionGroupId;
    }
}
