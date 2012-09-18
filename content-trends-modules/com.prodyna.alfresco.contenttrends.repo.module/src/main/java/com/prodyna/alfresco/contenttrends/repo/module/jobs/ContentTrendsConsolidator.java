/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.MD5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prodyna.alfresco.contenttrends.repo.module.audit.ContentTrendsEventType;
import com.prodyna.alfresco.contenttrends.repo.module.model.ContentTrendsModel;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ContentTrendsConsolidator extends AbstractContentTrendsProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTrendsConsolidator.class);

    protected static final String ROOT_PATH = "/ContentTrends/Aggregated";

    protected static final String[] AFFECTED_NODE_VALUE_KEYS = { "affectedPrimaryNode", "affectedSecondaryNode", "affectedTertiaryNode" };
    protected static final Pattern AFFECTED_NODE_VALUE_PATTERN;
    static
    {
        final StringBuilder patternBuilder = new StringBuilder();
        for (final String affectedNodeValueKey : AFFECTED_NODE_VALUE_KEYS)
        {
            if (patternBuilder.length() != 0)
            {
                patternBuilder.append("|");
            }
            patternBuilder.append(affectedNodeValueKey);
        }

        AFFECTED_NODE_VALUE_PATTERN = Pattern.compile("^/ContentTrendsBase/.+/(" + patternBuilder + ")$");
    }

    protected static final QName LOCK_QNAME = QName.createQName(ContentTrendsModel.NAMESPACE_URI, "ContentTrendsConsolidator");
    protected static final long LOCK_TTL = 1000 * 30;

    public final void consolidateEvents()
    {
        super.processImpl();
    }

    @Override
    protected QName getLockQName()
    {
        return LOCK_QNAME;
    }

    @Override
    protected long getLockTTL()
    {
        return LOCK_TTL;
    }

    /**
     * Consolidate all audit entries in the ContentTrendsBase application by user and affected node
     */
    @Override
    protected void doProcess()
    {
        // first: delete superfluous entries (that should have been snatched by an audit filter)

        final AuditEntryIdCollector systemEventCollector = new AuditEntryIdCollector();
        final AuditQueryParameters queryParams = new AuditQueryParameters();
        queryParams.setApplicationName("ContentTrendsBase");
        queryParams.setForward(true);
        queryParams.setUser(AuthenticationUtil.getSystemUserName());
        this.auditService.auditQuery(systemEventCollector, queryParams, -1);

        this.auditService.clearAudit(systemEventCollector.getAuditEntryIds());

        // second: collect unique userNames

        final UserCollector userCollector = new UserCollector();
        queryParams.setUser(null);
        this.auditService.auditQuery(userCollector, queryParams, -1);

        final Set<String> userNames = userCollector.getUserNames();

        // third: consolidate events per user, affected node and time

        for (final String userName : userNames)
        {
            conslidateUserEvents(userName);
        }
    }

    protected void conslidateUserEvents(final String userName)
    {
        final AuditQueryParameters queryParams = new AuditQueryParameters();
        queryParams.setApplicationName("ContentTrendsBase");
        queryParams.setForward(true);
        queryParams.setUser(userName);

        final TxnNodeEventAggregator aggregator = new TxnNodeEventAggregator();

        // TODO: Bug in audit query logic => persisted enums are only mapped correctly if maxResults > 0
        this.auditService.auditQuery(aggregator, queryParams, Integer.MAX_VALUE);

        final Map<Date, Map<NodeRef, Map<ContentTrendsEventType, AtomicLong>>> nodeEventCountsPerDateInterval = new HashMap<Date, Map<NodeRef, Map<ContentTrendsEventType, AtomicLong>>>();
        final Map<Date, Map<NodeRef, Collection<NodeRef>>> workingCopySubsumationsPerDateInterval = new HashMap<Date, Map<NodeRef, Collection<NodeRef>>>();
        final Map<Object, TransactionData> transactionalData = aggregator.getTransactionalData();

        aggregateEventCountsFromTransactions(nodeEventCountsPerDateInterval, transactionalData);
        aggregateWorkingCopySubsumationsFromTransactions(workingCopySubsumationsPerDateInterval, transactionalData);

        // now record each date + node combination into our long term audit application
        final Map<String, Serializable> auditData = new HashMap<String, Serializable>();

        // TODO: A better strategy for hiding the identity - if possible
        // (since this needs to be reproducible for later scoring, it is always technically possible to find the consolidate entries for a
        // specific user by replaying the algorithm)
        auditData.put("user", MD5.Digest(userName.getBytes()));
        
        for (final Entry<Date, Map<NodeRef, Map<ContentTrendsEventType, AtomicLong>>> nodeEventCountsEntry : nodeEventCountsPerDateInterval
                .entrySet())
        {
            auditData.put("hour", ISO8601DateFormat.format(nodeEventCountsEntry.getKey()));

            final Map<NodeRef, Collection<NodeRef>> subsumedWorkingCopiesByNode = workingCopySubsumationsPerDateInterval
                    .get(nodeEventCountsEntry.getKey());

            for (final Entry<NodeRef, Map<ContentTrendsEventType, AtomicLong>> eventCountEntry : nodeEventCountsEntry.getValue().entrySet())
            {
                boolean anyEvent = false;
                final NodeRef nodeRef = eventCountEntry.getKey();
                auditData.put("nodeRef", nodeRef);
                final Map<ContentTrendsEventType, AtomicLong> counts = eventCountEntry.getValue();
                for (final ContentTrendsEventType eventType : ContentTrendsEventType.values())
                {
                    final AtomicLong count = counts.get(eventType);
                    if (count == null)
                    {
                        auditData.remove(eventType.name());
                    }
                    else
                    {
                        anyEvent = true;
                        auditData.put(eventType.name(), count.get());
                    }
                }

                if (subsumedWorkingCopiesByNode != null && subsumedWorkingCopiesByNode.containsKey(nodeRef))
                {
                    auditData.put("checkedInWorkingCopies", (Serializable) subsumedWorkingCopiesByNode.get(nodeRef));
                }
                else
                {
                    auditData.remove("checkedInWorkingCopies");
                }

                if (anyEvent)
                {
                    this.auditComponent.recordAuditValues(ROOT_PATH, auditData);
                }
            }
        }

        // clear our temporary data
        this.auditService.clearAudit(aggregator.getEntryIds());
    }

    private void aggregateWorkingCopySubsumationsFromTransactions(
            final Map<Date, Map<NodeRef, Collection<NodeRef>>> workingCopySubsumationsPerDateInterval,
            final Map<Object, TransactionData> transactionalData)
    {
        for (final Entry<Object, TransactionData> entry : transactionalData.entrySet())
        {
            final TransactionData txnData = entry.getValue();

            Map<NodeRef, Collection<NodeRef>> subsumedWorkingCopiesByNode = workingCopySubsumationsPerDateInterval.get(txnData
                    .getTxnHourDate());

            final Collection<NodeRef> affectedNodes = txnData.getAffectedNodes();
            for (final NodeRef affectedNode : affectedNodes)
            {
                final Collection<NodeRef> subsumedWorkingCopies = txnData.getSubsumedWorkingCopies(affectedNode);

                if (subsumedWorkingCopies != null && !subsumedWorkingCopies.isEmpty())
                {
                    if (subsumedWorkingCopiesByNode == null)
                    {
                        subsumedWorkingCopiesByNode = new HashMap<NodeRef, Collection<NodeRef>>();
                        workingCopySubsumationsPerDateInterval.put(txnData.getTxnHourDate(), subsumedWorkingCopiesByNode);
                    }

                    Collection<NodeRef> subsumedTotal = subsumedWorkingCopiesByNode.get(affectedNode);
                    if (subsumedTotal == null)
                    {
                        subsumedTotal = new HashSet<NodeRef>();
                        subsumedWorkingCopiesByNode.put(affectedNode, subsumedTotal);
                    }
                    subsumedTotal.addAll(subsumedWorkingCopies);
                }
            }
        }
    }

    private void aggregateEventCountsFromTransactions(
            final Map<Date, Map<NodeRef, Map<ContentTrendsEventType, AtomicLong>>> nodeEventCountsPerDateInterval,
            final Map<Object, TransactionData> transactionalData)
    {
        for (final Entry<Object, TransactionData> entry : transactionalData.entrySet())
        {
            final TransactionData txnData = entry.getValue();

            Map<NodeRef, Map<ContentTrendsEventType, AtomicLong>> nodeEventCounts = nodeEventCountsPerDateInterval.get(txnData
                    .getTxnHourDate());
            if (nodeEventCounts == null)
            {
                nodeEventCounts = new HashMap<NodeRef, Map<ContentTrendsEventType, AtomicLong>>();
                nodeEventCountsPerDateInterval.put(txnData.getTxnHourDate(), nodeEventCounts);
            }

            final Collection<NodeRef> affectedNodes = txnData.getAffectedNodes();
            for (final NodeRef affectedNode : affectedNodes)
            {
                final Collection<ContentTrendsEventType> consolidatedEvents = txnData.getConsolidatedEvents(affectedNode);
                if (!consolidatedEvents.isEmpty())
                {
                    Map<ContentTrendsEventType, AtomicLong> counts = nodeEventCounts.get(affectedNode);
                    if (counts == null)
                    {
                        counts = new EnumMap<ContentTrendsEventType, AtomicLong>(ContentTrendsEventType.class);
                        nodeEventCounts.put(affectedNode, counts);
                    }

                    for (final ContentTrendsEventType event : consolidatedEvents)
                    {
                        final AtomicLong count = counts.get(event);
                        if (count == null)
                        {
                            counts.put(event, new AtomicLong(1));
                        }
                        else
                        {
                            count.incrementAndGet();
                        }
                    }
                }
            }
        }
    }

    protected static class UserCollector implements AuditQueryCallback
    {

        private final Set<String> userNames = new HashSet<String>();

        protected Set<String> getUserNames()
        {
            return Collections.unmodifiableSet(this.userNames);
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean valuesRequired()
        {
            return false;
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean handleAuditEntry(final Long entryId, final String applicationName, final String user, final long time,
                final Map<String, Serializable> values)
        {
            this.userNames.add(user);
            return true;
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean handleAuditEntryError(final Long entryId, final String errorMsg, final Throwable error)
        {
            LOGGER.warn("Error handling audit event (): ()", entryId, errorMsg);

            // continue
            return true;
        }

    }

    protected static class TxnNodeEventAggregator implements AuditQueryCallback
    {

        private final Map<Object, TransactionData> transactionalData = new LinkedHashMap<Object, TransactionData>();
        private final List<Long> entryIds = new ArrayList<Long>();

        protected List<Long> getEntryIds()
        {
            return Collections.unmodifiableList(this.entryIds);
        }

        protected Map<Object, TransactionData> getTransactionalData()
        {
            return Collections.unmodifiableMap(this.transactionalData);
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean valuesRequired()
        {
            return true;
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean handleAuditEntry(final Long entryId, final String applicationName, final String user, final long time,
                final Map<String, Serializable> values)
        {
            Serializable txnId = null;
            for (final String key : values.keySet())
            {
                if (key.endsWith("/txn"))
                {
                    txnId = values.get(key);
                    break;
                }
            }

            TransactionData txnData = this.transactionalData.get(txnId);
            if (txnData == null)
            {
                txnData = new TransactionData(time);
                this.transactionalData.put(txnId, txnData);
            }

            final Collection<NodeRef> affectedNodes = new HashSet<NodeRef>();
            collectedAffectedNodes(values, affectedNodes);

            final Collection<ContentTrendsEventType> events = EnumSet.noneOf(ContentTrendsEventType.class);
            collectContentTrendsEvents(values, events);

            NodeRef workingCopy = null;
            for (final String key : values.keySet())
            {
                if (key.endsWith("/workingCopyNode"))
                {
                    workingCopy = new NodeRef((String) values.get(key));
                    break;
                }
            }

            for (final NodeRef node : affectedNodes)
            {
                for (final ContentTrendsEventType eventType : events)
                {
                    txnData.addEvent(node, eventType);
                }

                if (workingCopy != null)
                {
                    txnData.addSubsumedWorkingCopy(node, workingCopy);
                }
            }

            this.entryIds.add(entryId);
            return true;
        }

        protected void collectedAffectedNodes(final Map<String, Serializable> values, final Collection<NodeRef> affectedNodes)
        {
            for (final String key : values.keySet())
            {
                if (AFFECTED_NODE_VALUE_PATTERN.matcher(key).matches())
                {
                    final Serializable affectedNodeValue = values.get(key);

                    if (affectedNodeValue instanceof NodeRef)
                    {
                        affectedNodes.add((NodeRef) affectedNodeValue);
                    }
                    else if (affectedNodeValue instanceof Collection<?>)
                    {
                        for (final Object affectedNodeValueElement : ((Collection<?>) affectedNodeValue))
                        {
                            if (affectedNodeValueElement instanceof NodeRef)
                            {
                                affectedNodes.add((NodeRef) affectedNodeValueElement);
                            }
                        }
                    }
                }
            }
        }

        protected void collectContentTrendsEvents(final Map<String, Serializable> values, final Collection<ContentTrendsEventType> events)
        {
            Serializable eventValue = null;
            for (final String key : values.keySet())
            {
                if (key.endsWith("/event"))
                {
                    eventValue = values.get(key);
                    break;
                }
            }

            if (eventValue != null)
            {
                // this is a simple entry
                if (eventValue instanceof ContentTrendsEventType)
                {
                    events.add((ContentTrendsEventType) eventValue);
                }
                else if (eventValue instanceof Collection<?>)
                {
                    for (final Object eventValueElement : (Collection<?>) eventValue)
                    {
                        if (eventValueElement instanceof ContentTrendsEventType)
                        {
                            events.add((ContentTrendsEventType) eventValueElement);
                        }
                    }
                }
            }
            else
            {
                Serializable nodeEventValue = values.get("nodeEvent");
                Serializable propertyEventValue = values.get("propertyEvent");
                Serializable aspectEventValue = values.get("aspectEvent");

                for (final String key : values.keySet())
                {
                    if (key.endsWith("/nodeEvent"))
                    {
                        nodeEventValue = values.get(key);
                    }
                    else if (key.endsWith("/propertyEvent"))
                    {
                        propertyEventValue = values.get(key);
                    }
                    else if (key.endsWith("/aspectEvent"))
                    {
                        aspectEventValue = values.get(key);
                    }
                }

                final ContentTrendsEventType nodeEvent = (nodeEventValue instanceof ContentTrendsEventType) ? (ContentTrendsEventType) nodeEventValue
                        : null;
                final Collection<ContentTrendsEventType> propertyEvents;
                if (propertyEventValue instanceof ContentTrendsEventType)
                {
                    propertyEvents = EnumSet.of((ContentTrendsEventType) propertyEventValue);
                }
                else if (propertyEventValue instanceof Collection<?>)
                {
                    propertyEvents = EnumSet.noneOf(ContentTrendsEventType.class);
                    for (final Object propertyEventValueElement : (Collection<?>) propertyEventValue)
                    {
                        if (propertyEventValueElement instanceof ContentTrendsEventType)
                        {
                            propertyEvents.add((ContentTrendsEventType) propertyEventValueElement);
                        }
                    }
                }
                else
                {
                    propertyEvents = null;
                }
                final ContentTrendsEventType aspectEvent = (aspectEventValue instanceof ContentTrendsEventType) ? (ContentTrendsEventType) aspectEventValue
                        : null;

                // node event should always be set
                if (nodeEvent != null)
                {
                    final Collection<ContentTrendsEventType> interpolatedEvents = ContentTrendsEventType.interpolate(nodeEvent,
                            propertyEvents, aspectEvent);
                    if (interpolatedEvents != null)
                    {
                        events.addAll(interpolatedEvents);
                    }
                }
            }
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean handleAuditEntryError(final Long entryId, final String errorMsg, final Throwable error)
        {
            LOGGER.warn("Error handling audit event (): ()", entryId, errorMsg);

            // continue
            return true;
        }
    }

    protected static class TransactionData
    {

        private final Date txnHourDate;
        private final Map<NodeRef, Collection<ContentTrendsEventType>> nodeEvents = new HashMap<NodeRef, Collection<ContentTrendsEventType>>();
        private final Map<NodeRef, Collection<NodeRef>> subsumedWorkingCopies = new HashMap<NodeRef, Collection<NodeRef>>();

        protected TransactionData(final long firstEventDate)
        {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(firstEventDate);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            this.txnHourDate = calendar.getTime();
        }

        protected void addSubsumedWorkingCopy(final NodeRef affectedNode, final NodeRef workingCopy)
        {
            Collection<NodeRef> workingCopies = this.subsumedWorkingCopies.get(affectedNode);
            if (workingCopies == null)
            {
                workingCopies = new HashSet<NodeRef>();
                this.subsumedWorkingCopies.put(affectedNode, workingCopies);
            }
            workingCopies.add(workingCopy);
        }

        protected void addEvent(final NodeRef affectedNode, final ContentTrendsEventType eventType)
        {
            Collection<ContentTrendsEventType> events = this.nodeEvents.get(affectedNode);
            if (events == null)
            {
                events = EnumSet.of(eventType);
                this.nodeEvents.put(affectedNode, events);
            }
            else
            {
                events.add(eventType);
            }

        }

        protected Date getTxnHourDate()
        {
            return this.txnHourDate;
        }

        protected Collection<NodeRef> getAffectedNodes()
        {
            return Collections.unmodifiableSet(this.nodeEvents.keySet());
        }

        protected Collection<NodeRef> getSubsumedWorkingCopies(final NodeRef nodeRef)
        {
            return this.subsumedWorkingCopies.get(nodeRef);
        }

        protected Collection<ContentTrendsEventType> getConsolidatedEvents(final NodeRef nodeRef)
        {
            final Collection<ContentTrendsEventType> consolidatedEvents = EnumSet.noneOf(ContentTrendsEventType.class);

            final Collection<ContentTrendsEventType> events = this.nodeEvents.get(nodeRef);
            final Collection<ContentTrendsEventType> subsumedEvents = EnumSet.noneOf(ContentTrendsEventType.class);

            for (final ContentTrendsEventType eventUnderConsideration : events)
            {
                subsumedEvents.clear();
                boolean alreadySubsumed = false;

                for (final ContentTrendsEventType consolidatedEvent : consolidatedEvents)
                {

                    if (consolidatedEvent.subsumes(eventUnderConsideration))
                    {
                        alreadySubsumed = true;
                    }
                    else if (eventUnderConsideration.subsumes(consolidatedEvent))
                    {
                        subsumedEvents.add(consolidatedEvent);
                    }
                }

                if (!alreadySubsumed)
                {
                    consolidatedEvents.add(eventUnderConsideration);
                    consolidatedEvents.removeAll(subsumedEvents);
                }
            }

            return consolidatedEvents;
        }
    }
}
