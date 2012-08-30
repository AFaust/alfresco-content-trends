/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.text.MessageFormat;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.audit.extractor.AbstractDataExtractor;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractAffectedContentNodeExtractor extends AbstractDataExtractor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAffectedContentNodeExtractor.class);

    // XPath pattern with full qualified names for CPU-efficient matching (prefix path strings are expensive)
    protected static final String SURF_CONFIG_PATTERN = MessageFormat.format(
            "^/\\'{'{0}\\}company_home/\\'{'{1}\\}sites(/[^}]+\\}[^/]+)?/\\'{'{2}\\}surf-config(/[^}]+\\}[^/]+)*$",
            ApplicationModel.TYPE_CONFIGURATIONS.getNamespaceURI(), SiteModel.TYPE_SITES.getNamespaceURI(),
            ContentModel.TYPE_CONTENT.getNamespaceURI());

    protected NodeService nodeService;
    protected DictionaryService dictionaryService;

    protected NodeRef findAffectedContentNode(final NodeRef baseNode)
    {
        NodeRef affectedContentNode = null;

        if (this.nodeService.exists(baseNode))
        {
            // eliminate surf configuration files (runtime configuration content should never be tracked)
            // TODO: are there any other configuration content elements in need of blocking?
            final Path primaryPath = this.nodeService.getPath(baseNode);
            // NOTE: To avoid adding to much overhead by auditing, we do not use the prefixed string here (it would be quite expensive to do
            // so)
            if (!primaryPath.toString().matches(SURF_CONFIG_PATTERN))
            {
                NodeRef currentNode = baseNode;

                while (currentNode != null)
                {
                    final QName nodeType = this.nodeService.getType(currentNode);

                    if (this.nodeService.hasAspect(currentNode, RenditionModel.ASPECT_RENDITION)
                            || this.dictionaryService.isSubClass(nodeType, ContentModel.TYPE_THUMBNAIL))
                    {
                        // we don't consider renditions / thumbnails "affected"
                        break;
                    }

                    if (this.dictionaryService.isSubClass(nodeType, ContentModel.TYPE_CONTENT))
                    {
                        affectedContentNode = currentNode;
                    }

                    currentNode = this.nodeService.getPrimaryParent(currentNode).getParentRef();
                }
            }
        }
        else
        {
            LOGGER.warn(MessageFormat.format("Event audited for non-existing node: {0} -- Check audit configuration!", baseNode),
                    new RuntimeException());
        }

        return affectedContentNode;
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
