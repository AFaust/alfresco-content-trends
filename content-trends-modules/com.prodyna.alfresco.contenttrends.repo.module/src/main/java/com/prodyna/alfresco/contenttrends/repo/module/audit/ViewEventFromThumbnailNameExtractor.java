/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;

import org.alfresco.repo.audit.extractor.AbstractDataExtractor;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ViewEventFromThumbnailNameExtractor extends AbstractDataExtractor
{
    private static final String DOC_LIB_THUMBNAIL_NAME = "doclib";
    private ThumbnailService thumbnailService;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(Serializable data)
    {
        final boolean supported = data instanceof String;
        return supported;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable extractData(Serializable value) throws Throwable
    {
        final String thumbnailName = (String) value;
        final ContentTrendsEventType result;

        if (thumbnailName != null)
        {
            final ThumbnailDefinition thumbnailDefinition = this.thumbnailService.getThumbnailRegistry().getThumbnailDefinition(
                    thumbnailName);

            result = (thumbnailDefinition != null && !DOC_LIB_THUMBNAIL_NAME.equals(thumbnailName)) ? ContentTrendsEventType.VIEW : null;
        }
        else
        {
            result = null;
        }

        return result;
    }

    /**
     * @param thumbnailService
     *            the thumbnailService to set
     */
    public void setThumbnailService(ThumbnailService thumbnailService)
    {
        this.thumbnailService = thumbnailService;
    }

}
