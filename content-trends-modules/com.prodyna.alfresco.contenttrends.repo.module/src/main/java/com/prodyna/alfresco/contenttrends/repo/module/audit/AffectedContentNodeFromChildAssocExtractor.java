/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class AffectedContentNodeFromChildAssocExtractor extends AbstractAffectedContentNodeExtractor
{

    protected boolean parentRef = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(Serializable data)
    {
        final boolean supported;

        if (data instanceof ChildAssociationRef)
        {
            supported = true;
        }
        else if (data instanceof Collection<?>)
        {
            boolean allElementsChildAssocs = true;
            for (final Object element : (Collection<?>) data)
            {
                allElementsChildAssocs = allElementsChildAssocs && (element instanceof ChildAssociationRef);
            }
            supported = allElementsChildAssocs;
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
        if (value instanceof ChildAssociationRef)
        {
            result = findAffectedContentNode((ChildAssociationRef) value);
        }
        else if (value instanceof Collection<?>)
        {
            final Collection<NodeRef> nodes = new HashSet<NodeRef>();
            for (final Object element : (Collection<?>) value)
            {
                if (element instanceof ChildAssociationRef)
                {
                    final NodeRef contentNode = findAffectedContentNode((ChildAssociationRef) element);
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

    protected NodeRef findAffectedContentNode(final ChildAssociationRef baseChildAssoc)
    {
        final NodeRef baseNode = this.parentRef ? baseChildAssoc.getParentRef() : baseChildAssoc.getChildRef();
        final NodeRef affectedContentNode = super.findAffectedContentNode(baseNode);
        return affectedContentNode;
    }

    /**
     * @param parentRef
     *            the parentRef to set
     */
    public void setParentRef(boolean parentRef)
    {
        this.parentRef = parentRef;
    }

}
