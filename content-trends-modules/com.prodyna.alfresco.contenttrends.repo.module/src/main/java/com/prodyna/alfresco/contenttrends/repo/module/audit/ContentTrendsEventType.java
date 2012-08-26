/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public enum ContentTrendsEventType
{

    // can not use an EnumSet since at construction time, enum class is not fully initialized
    /** Viewing as determined by access to non-doclib thumbnail */
    VIEW(),
    /** Download as determined by content access in a readonly transaction with a known download endpoint in context */
    DOWNLOAD(),
    /** Simple node edit (classification, metadata, associations, location) */
    EDIT(),
    /** Semantic node edit */
    TAG(),
    /** Content node edit */
    EDIT_CONTENT(Arrays.asList(ContentTrendsEventType.EDIT)),
    /** Pure lock-events should be discounted, so they need to be distinguishable from regular edits */
    LOCK(Arrays.asList(ContentTrendsEventType.EDIT)),
    /** Social comment event */
    COMMENT(Arrays.asList(ContentTrendsEventType.EDIT)),
    /** Social rating event */
    RATE(Arrays.asList(ContentTrendsEventType.EDIT)),
    /** Cancellation marker in order to discount prior COCI events */
    CANCEL_COCI(Arrays.asList(ContentTrendsEventType.EDIT, ContentTrendsEventType.LOCK)),
    /** Copy event (Note: copy events may in the future open up propagation chains for trend scores) */
    COPY(Arrays.asList(ContentTrendsEventType.TAG, ContentTrendsEventType.EDIT_CONTENT, ContentTrendsEventType.LOCK,
            ContentTrendsEventType.RATE, ContentTrendsEventType.COMMENT)),
    /** Major edit initialization event */
    COCI(Arrays.asList(ContentTrendsEventType.COPY));

    // can not use an EnumSetsince at construction time, enum class is not fully initialized
    private final Collection<ContentTrendsEventType> subsumedEventTypes = new HashSet<ContentTrendsEventType>();

    private ContentTrendsEventType(final Collection<ContentTrendsEventType> subsumesEventTypes)
    {
        this.subsumedEventTypes.addAll(subsumesEventTypes);
        for (final ContentTrendsEventType eventType : subsumesEventTypes)
        {
            this.subsumedEventTypes.addAll(eventType.subsumedEventTypes);
        }
    }

    private ContentTrendsEventType()
    {
        // NO-OP
    }

    public boolean subsumes(final ContentTrendsEventType eventType)
    {
        return this.subsumedEventTypes.contains(eventType);
    }

    public static Collection<ContentTrendsEventType> interpolate(final ContentTrendsEventType nodeEventType,
            final Collection<ContentTrendsEventType> propertyEventTypes, final ContentTrendsEventType aspectEventType)
    {
        // aspect event is only provided if an aspect related operation in NodeService triggered the event
        // property is provided if any property change occured in a NodeService triggered event (null only if no properties actually changed)

        final Collection<ContentTrendsEventType> actualEvents;

        switch (nodeEventType)
        {
        case COMMENT:
            if (propertyEventTypes != null)
            {
                // we don't consider tagging, property or other events on comment nodes a valid event
                actualEvents = propertyEventTypes.contains(ContentTrendsEventType.EDIT_CONTENT) ? EnumSet.of(COMMENT) : null;
            }
            else
            {
                // other constellations than the above are unexpected / invalid events
                actualEvents = null;
            }
            break;
        case RATE:
            if (propertyEventTypes != null)
            {
                // we don't consider tagging, content or other events on rating nodes a valid event
                // TODO: protected against arbitrary property changes on rating nodes
                actualEvents = propertyEventTypes.contains(ContentTrendsEventType.EDIT) ? EnumSet.of(RATE) : null;
            }
            else
            {
                // other constellations than the above are unexpected / invalid events
                actualEvents = null;
            }
            break;
        case EDIT:
            // we could not determine a more precise event based on type
            if (aspectEventType != null)
            {
                switch (aspectEventType)
                {
                case TAG:
                    actualEvents = EnumSet.of(TAG);
                    break;
                default:
                    // property event may be null, so default to EDIT
                    actualEvents = propertyEventTypes != null ? propertyEventTypes : EnumSet.of(EDIT);
                }
            }
            else
            {
                // null aspect event => property event is the best source of information
                actualEvents = propertyEventTypes;
            }
            break;
        default:
            actualEvents = EnumSet.of(EDIT);
        }

        return actualEvents;
    }
}
