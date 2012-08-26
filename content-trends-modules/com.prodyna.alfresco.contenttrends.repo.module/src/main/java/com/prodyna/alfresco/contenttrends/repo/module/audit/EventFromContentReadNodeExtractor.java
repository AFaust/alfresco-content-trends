/*
* Copyright 2012 PRODYNA AG
*
* Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.opensource.org/licenses/eclipse-1.0.php or
* http://www.nabucco.org/License.html
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.audit.extractor.AbstractDataExtractor;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.namespace.QName;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 * @deprecated Can't count correctly due to variations in content access / download 
 */
@Deprecated
public class EventFromContentReadNodeExtractor extends AbstractDataExtractor
{
    private final static Set<String> KNOWN_DOWNLOAD_ENDPOINT_NAMES;
    static
    {
        final Set<String> endpointNames = new HashSet<String>();

        endpointNames.add("org.alfresco.repo.web.scripts.content.StreamContent");
        endpointNames.add("org.alfresco.repo.web.scripts.content.ContentGet");
        endpointNames.add("org.alfresco.web.app.servlet.BaseDownloadContentServlet");
        endpointNames.add("org.alfresco.web.app.servlet.DownloadContentServlet");
        endpointNames.add("org.alfresco.web.app.servlet.GuestDownloadContentServlet");
        endpointNames.add("org.alfresco.web.app.servlet.DownloadRawContentServlet");

        KNOWN_DOWNLOAD_ENDPOINT_NAMES = Collections.unmodifiableSet(endpointNames);
    }

    private static final String DOC_LIB_THUMBNAIL_NAME = "doclib";

    private ThumbnailService thumbnailService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;

    // TODO: actual read tracking => count as download when full size of content has been transferred once in a certain period of time 

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(Serializable data)
    {
        final boolean supported = data instanceof NodeRef;
        return supported;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable extractData(Serializable value) throws Throwable
    {
        final NodeRef node = (NodeRef) value;
        final boolean isRendition = this.nodeService.hasAspect(node, RenditionModel.ASPECT_RENDITION);
        final boolean isNonDocLibRendition;

        if (isRendition)
        {
            final String localName = this.nodeService.getPrimaryParent(node).getQName().getLocalName();
            final ThumbnailDefinition thumbnailDefinition = this.thumbnailService.getThumbnailRegistry().getThumbnailDefinition(localName);
            isNonDocLibRendition = thumbnailDefinition != null && !DOC_LIB_THUMBNAIL_NAME.equals(localName);
        }
        else
        {
            isNonDocLibRendition = false;
        }

        final ContentTrendsEventType result;

        final QName nodeTypeQName = this.nodeService.getType(node);
        if (this.dictionaryService.isSubClass(nodeTypeQName, ContentModel.TYPE_CONTENT))
        {
            boolean isDownload = false;
            /*
             * unfortunately, at the moment we don't know of a better way to determine if a download endpoint is currently in use other than
             * by examining the stack frames of the current thread
             */
            final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            // we begin at the tail because we expect to find a potential endpoint earlier from that end
            for (int i = stackTrace.length - 1; !isDownload && i > 0; i--)
            {
                final StackTraceElement element = stackTrace[i];
                final String className = element.getClassName();

                isDownload = KNOWN_DOWNLOAD_ENDPOINT_NAMES.contains(className);
            }

            if (isDownload)
            {
                if (isRendition)
                {
                    // download of a doclib rendition doesn't count at all while other renditions count towards viewing
                    result = isNonDocLibRendition ? ContentTrendsEventType.VIEW : null;
                }
                else
                {
                    result = ContentTrendsEventType.DOWNLOAD;
                }
            }
            else
            {
                result = null;
            }
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
