/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class AffectedContentNodeExtractor extends AbstractAffectedContentNodeExtractor
{

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(Serializable data)
    {
        final boolean supported;

        if (data instanceof NodeRef)
        {
            supported = true;
        }
        else if (data instanceof Collection<?>)
        {
            boolean allElementsNodeRefs = true;
            for (final Object element : (Collection<?>) data)
            {
                allElementsNodeRefs = allElementsNodeRefs && (element instanceof NodeRef);
            }
            supported = allElementsNodeRefs;
        }
        else
        {
            supported = false;
        }

        return supported;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable extractData(Serializable value) throws Throwable
    {
        final Serializable result;
        if (value instanceof NodeRef)
        {
            result = findAffectedContentNode((NodeRef) value);
        }
        else if (value instanceof Collection<?>)
        {
            final Collection<NodeRef> nodes = new HashSet<NodeRef>();
            for (final Object element : (Collection<?>) value)
            {
                if (element instanceof NodeRef)
                {
                    final NodeRef contentNode = findAffectedContentNode((NodeRef) element);
                    if (contentNode != null)
                    {
                        nodes.add(contentNode);
                    }
                }
            }

            if (nodes.isEmpty())
            {
                result = null;
            }
            else
            {
                result = (Serializable) nodes;
            }
        }
        else
        {
            result = null;
        }

        return result;
    }

}
