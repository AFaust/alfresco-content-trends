/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.audit.extractor.AbstractDataExtractor;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class NodeEventFromAspectExtractor extends AbstractDataExtractor
{
    private DictionaryService dictionaryService;

    @Override
    public boolean isSupported(Serializable data)
    {
        final boolean supported;

        if (data instanceof QName)
        {
            final QName qname = (QName) data;
            supported = this.dictionaryService.getAspect(qname) != null;
        }
        else
        {
            supported = false;
        }

        return supported;
    }

    @Override
    public Serializable extractData(Serializable value) throws Throwable
    {
        final Serializable data;

        if (value instanceof QName)
        {
            // can only be removeAspect at this point
            final QName aspectTypeQName = (QName) value;

            if (ContentModel.ASPECT_TAGGABLE.equals(aspectTypeQName))
            {
                data = ContentTrendsEventType.TAG;
            }
            else if (ForumModel.ASPECT_DISCUSSABLE.equals(aspectTypeQName))
            {
                data = ContentTrendsEventType.COMMENT;
            }
            else
            {
                data = ContentTrendsEventType.EDIT;
            }
        }
        else
        {
            data = null;
        }

        return data;
    }

    /**
     * @param dictionaryService
     *            the dictionaryService to set
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

}
