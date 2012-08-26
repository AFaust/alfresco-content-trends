/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.audit.extractor.AbstractDataExtractor;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class NodeEventFromNodeOrTypeExtractor extends AbstractDataExtractor
{

    private NodeService nodeService;
    private DictionaryService dictionaryService;

    @Override
    public boolean isSupported(Serializable data)
    {
        final boolean result;

        if (data instanceof NodeRef)
        {
            result = true;
        }
        else if (data instanceof QName)
        {
            final QName qname = (QName) data;
            result = this.dictionaryService.getType(qname) != null;
        }
        else
        {
            result = false;
        }

        return result;
    }

    @Override
    public Serializable extractData(Serializable value) throws Throwable
    {
        final QName typeQName;

        if (value instanceof NodeRef)
        {
            typeQName = this.nodeService.getType((NodeRef) value);
        }
        else if (value instanceof QName)
        {
            typeQName = (QName) value;
        }
        else
        {
            typeQName = null;
        }

        final Serializable data;
        if (typeQName != null)
        {
            if (this.dictionaryService.isSubClass(typeQName, ContentModel.TYPE_RATING))
            {
                data = ContentTrendsEventType.RATE;
            }
            else if (ForumModel.TYPE_FORUM.getNamespaceURI().equalsIgnoreCase(typeQName.getNamespaceURI()))
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
     * @param nodeService
     *            the nodeService to set
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
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
