/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.audit.extractor.AbstractDataExtractor;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class NodeEventFromPropertiesExtractor extends AbstractDataExtractor
{
    private DictionaryService dictionaryService;

    @Override
    public boolean isSupported(Serializable data)
    {
        final boolean supported;

        if (data instanceof QName)
        {
            final QName qname = (QName) data;
            supported = this.dictionaryService.getProperty(qname) != null;
        }
        else if (data instanceof Map<?, ?>)
        {
            boolean propertyMap = false;
            for (final Object key : ((Map<?, ?>) data).keySet())
            {
                final boolean isProperty = (key instanceof QName) && this.dictionaryService.getProperty((QName) key) != null;
                propertyMap = propertyMap || isProperty;
            }

            supported = propertyMap;
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
            final QName nodePropertyQName = (QName) value;
            final PropertyDefinition propDef = this.dictionaryService.getProperty(nodePropertyQName);
            if (ContentModel.PROP_TAGS.equals(nodePropertyQName))
            {
                data = ContentTrendsEventType.TAG;
            }
            else if (propDef != null && propDef.getDataType().getName().equals(DataTypeDefinition.CONTENT))
            {
                data = ContentTrendsEventType.EDIT_CONTENT;
            }
            else
            {
                data = ContentTrendsEventType.EDIT;
            }
        }
        else if (value instanceof Map<?, ?>)
        {
            @SuppressWarnings("unchecked")
            final Map<QName, Serializable> properties = (Map<QName, Serializable>) value;

            final boolean tagsUpdated = properties.containsKey(ContentModel.PROP_TAGS);
            final boolean contentUpdated;
            {
                boolean propsContainContentProp = false;
                for (final QName propQName : properties.keySet())
                {
                    final PropertyDefinition propDef = this.dictionaryService.getProperty(propQName);
                    if (propDef != null)
                    {
                        propsContainContentProp = propDef.getDataType().getName().equals(DataTypeDefinition.CONTENT);

                        if (propsContainContentProp)
                        {
                            break;
                        }
                    }
                }
                contentUpdated = propsContainContentProp;
            }

            if (tagsUpdated && contentUpdated)
            {
                data = (Serializable) Arrays.asList(ContentTrendsEventType.EDIT_CONTENT, ContentTrendsEventType.TAG);
            }
            else if (contentUpdated)
            {
                // EDIT_CONTENT subsumes EDIT, so no need to check for property amount
                data = ContentTrendsEventType.EDIT_CONTENT;
            }
            else if (tagsUpdated && properties.size() > 1)
            {
                // TAG and EDIT are on-par priority-wise, so this special constellation needs to be checked
                data = (Serializable) Arrays.asList(ContentTrendsEventType.EDIT, ContentTrendsEventType.TAG);
            }
            else if (tagsUpdated)
            {
                data = ContentTrendsEventType.TAG;
            }
            else if (!properties.isEmpty())
            {
                // any properties have changed
                data = ContentTrendsEventType.EDIT;
            }
            else
            {
                data = null;
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
