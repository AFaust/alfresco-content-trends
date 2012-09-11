/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.prodyna.alfresco.contenttrends.repo.module.audit.ContentTrendsEventType;
import com.prodyna.alfresco.contenttrends.repo.module.model.ContentTrendsModel;
import com.prodyna.alfresco.contenttrends.repo.module.service.ContentTrendsService;
import com.prodyna.alfresco.contenttrends.repo.module.service.NodeScoreType;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ContentTrendsScoreUpdater extends AbstractContentTrendsProcessor implements InitializingBean
{
    private static final int SEARCH_BATCH_SIZE = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTrendsScoreUpdateJob.class);

    /*
     * 24 hours +- 12 hours is our lookup frame for historic scores, so max age needs to be 36 hours - scoring interval (can only be
     * determined at instance level)
     */
    protected static final long BACKSTEP_FOR_HISTORIC_SCORE = 24l * 60 * 60 * 1000;
    protected static final long HISTORIC_LOOKUP_WINDOW = 12l * 60 * 60 * 1000;
    protected static final long MAX_AGE_LAST_HISTORIC_SCORE = BACKSTEP_FOR_HISTORIC_SCORE + HISTORIC_LOOKUP_WINDOW;

    protected static final QName LOCK_QNAME = QName.createQName(ContentTrendsModel.NAMESPACE_URI, "ContentTrendsScoreUpdater");
    protected static final long LOCK_TTL = 1000 * 30;

    protected static final String AGGREGATED_CONTENT_TRENDS_ROOT_PATH = "/" + ContentTrendsService.CONTENT_TRENDS_AUDIT_APPLICATION
            + "/Aggregated";
    protected static final String AGGREGATED_USER_PATH = AGGREGATED_CONTENT_TRENDS_ROOT_PATH + "/user";
    protected static final String AGGREGATED_HOUR_PATH = AGGREGATED_CONTENT_TRENDS_ROOT_PATH + "/hour";
    protected static final String AGGREGATED_NODE_REF_PATH = AGGREGATED_CONTENT_TRENDS_ROOT_PATH + "/nodeRef";
    protected static final String AGGREGATED_CHECKED_IN_WORKING_COPIES_PATH = AGGREGATED_CONTENT_TRENDS_ROOT_PATH
            + "/checkedInWorkingCopies";
    protected static final String AGGREGATED_TAG_EVENT_PATH = AGGREGATED_CONTENT_TRENDS_ROOT_PATH + "/" + ContentTrendsEventType.TAG.name();

    private static final int DEFAULT_SCORING_WINDOW_SIZE_IN_DAY = 7;
    protected int scoringWindowSizeInDays = DEFAULT_SCORING_WINDOW_SIZE_IN_DAY;

    protected StoreRef storeToProcess = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
    protected NodeEventScoringStrategy scoringStrategy;

    protected SearchService searchService;
    protected NodeService nodeService;

    protected BehaviourFilter behaviourFilter;

    protected ContentTrendsConsolidator consolidator;

    protected int scoringIntervalInMins = (int) (HISTORIC_LOOKUP_WINDOW / (60 * 1000));

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        super.afterPropertiesSet();

        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "searchService", this.searchService);
        PropertyCheck.mandatory(this, "behaviourFilter", this.behaviourFilter);
        PropertyCheck.mandatory(this, "scoringStrategy", this.scoringStrategy);
        PropertyCheck.mandatory(this, "storeToProcess", this.storeToProcess);
    }

    /**
     * @param scoringIntervalInMins
     *            the scoringIntervalInMins to set
     */
    public void setScoringIntervalInMins(int scoringIntervalInMins)
    {
        if (scoringIntervalInMins >= 0)
        {
            this.scoringIntervalInMins = scoringIntervalInMins;
        }
    }

    /**
     * @param scoringWindowSizeInDays
     *            the scoringWindowSizeInDays to set
     */
    public void setScoringWindowSizeInDays(int scoringWindowSizeInDays)
    {
        if (scoringWindowSizeInDays > 0)
        {
            this.scoringWindowSizeInDays = scoringWindowSizeInDays;
        }
    }

    /**
     * @param storeToProcess
     *            the storeToProcess to set
     */
    public void setStoreToProcess(String storeToProcess)
    {
        this.storeToProcess = new StoreRef(storeToProcess);
    }

    /**
     * @param scoringStrategy
     *            the scoringStrategy to set
     */
    public void setScoringStrategy(NodeEventScoringStrategy scoringStrategy)
    {
        this.scoringStrategy = scoringStrategy;
    }

    /**
     * @param searchService
     *            the searchService to set
     */
    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
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
     * @param behaviourFilter
     *            the behaviourFilter to set
     */
    public void setBehaviourFilter(BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }

    /**
     * @param consolidator
     *            the consolidator to set
     */
    public void setConsolidator(ContentTrendsConsolidator consolidator)
    {
        this.consolidator = consolidator;
    }

    public void updateScores()
    {
        this.consolidator.consolidateEvents();
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
     * Score all content elements based on aggregated events in the ContentTrends audit application
     */
    @Override
    protected void doProcess()
    {

        final Calendar calendar = GregorianCalendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -this.scoringWindowSizeInDays);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        final long fromTime = calendar.getTimeInMillis();

        final Collection<NodeScores> updatedScores = calculatePreviouslyScoredContentScores(fromTime);
        final Collection<Long> processedAuditEntryIds = new HashSet<Long>();
        for (final NodeScores updatedScore : updatedScores)
        {
            processedAuditEntryIds.addAll(updatedScore.getAuditEntryIds());
        }

        final Collection<NodeScores> newScores = calculateUnscoredContentScores(fromTime, processedAuditEntryIds);

        final Collection<NodeScores> allScores = new HashSet<NodeScores>();
        allScores.addAll(updatedScores);
        allScores.addAll(newScores);

        final long date = System.currentTimeMillis();
        final long maxAgeHistoricScore = MAX_AGE_LAST_HISTORIC_SCORE - (this.scoringIntervalInMins * 60l * 1000);

        // TODO: two stage parallel execution
        // 1st stage: update scored content
        // 2nd stage: score content with events in audit logs (filter entries from 1st stage for efficiency)

        for (final NodeScores nodeScore : allScores)
        {
            final Pair<NodeScores, Long> historicScoreWithBackstep = findHistoricScoreWithBackstep(nodeScore.getNodeRef(), date);
            final Pair<NodeScores, Long> latestHistoricScore = findLatestHistoricScore(nodeScore.getNodeRef());

            final NodeRef nodeRef = nodeScore.getNodeRef();
            final boolean scoresChanged = saveScore(nodeRef, nodeScore, historicScoreWithBackstep != null ? historicScoreWithBackstep.getFirst()
                    : null);

            // only save historic score if there has been a change or we did not record scores for a while
            final long timeSinceLatestHistoricScore = latestHistoricScore != null ? date - latestHistoricScore.getSecond() : -1;
            if (latestHistoricScore == null || scoresChanged || timeSinceLatestHistoricScore > maxAgeHistoricScore)
            {
                // should we add a dummy "old" historic score entry in case of a change after skipping updates for a while?
                saveHistoricScore(nodeRef, nodeScore);
            }
        }
    }

    protected boolean saveScore(final NodeRef nodeRef, final NodeScores nodeScore, final NodeScores historicScore)
    {
        final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        for (final NodeScoreType scoreType : NodeScoreType.values())
        {
            final double score = nodeScore.getScore(scoreType);
            properties.put(scoreType.getScoreProperty(), (int) Math.round(score));
        }

        final Map<QName, Serializable> currentProperties = this.nodeService.getProperties(nodeRef);

        boolean scoresChanged = false;
        for (final QName prop : properties.keySet())
        {
            final Serializable before = currentProperties.get(prop);
            final Serializable after = properties.get(prop);

            scoresChanged = scoresChanged || !EqualsHelper.nullSafeEquals(before, after);
        }

        if (!scoresChanged)
        {
            properties.clear();
        }

        final Map<QName, Serializable> deltas = calculateScoreDeltas(nodeRef, nodeScore, historicScore);
        boolean deltasChanged = false;
        for (final QName prop : deltas.keySet())
        {
            final Serializable before = currentProperties.get(prop);
            final Serializable after = deltas.get(prop);

            deltasChanged = deltasChanged || !EqualsHelper.nullSafeEquals(before, after);
        }

        if (deltasChanged)
        {
            properties.putAll(deltas);
        }

        if (scoresChanged || deltasChanged)
        {
            this.behaviourFilter.disableBehaviour(nodeRef, ContentModel.ASPECT_AUDITABLE);
            this.behaviourFilter.disableBehaviour(nodeRef, ContentModel.ASPECT_VERSIONABLE);
            try
            {
                this.nodeService.addProperties(nodeRef, properties);
            }
            finally
            {
                this.behaviourFilter.enableBehaviour(nodeRef, ContentModel.ASPECT_AUDITABLE);
                this.behaviourFilter.enableBehaviour(nodeRef, ContentModel.ASPECT_VERSIONABLE);
            }
        }

        return scoresChanged;
    }

    protected Map<QName, Serializable> calculateScoreDeltas(final NodeRef nodeRef, final NodeScores nodeScore,
            final NodeScores historicScore)
    {
        final Map<QName, Serializable> deltas = new HashMap<QName, Serializable>();

        if (historicScore != null)
        {
            for (final NodeScoreType scoreType : NodeScoreType.values())
            {
                final double absoluteChange = nodeScore.getScore(scoreType) - historicScore.getScore(scoreType);
                final double relativeChange = 100 * absoluteChange / historicScore.getScore(scoreType);

                if (Double.isInfinite(relativeChange) || Double.isNaN(relativeChange))
                {
                    deltas.put(scoreType.getScoreChangeProperty(), null);
                }
                else
                {
                    deltas.put(scoreType.getScoreChangeProperty(), (int) Math.round(relativeChange));
                }
            }
        }
        else
        {
            for (final NodeScoreType scoreType : NodeScoreType.values())
            {
                deltas.put(scoreType.getScoreChangeProperty(), null);
            }
        }

        return deltas;
    }
    
    private Pair<NodeScores, Long> findLatestHistoricScore(final NodeRef nodeRef)
    {
        final FirstHistoricScoreFinder finder = new FirstHistoricScoreFinder(nodeRef);
        final AuditQueryParameters queryParams = new AuditQueryParameters();
        queryParams.setForward(false);
        queryParams.addSearchKey(ContentTrendsService.HISTORIC_NODE_REF_PATH, nodeRef);
        queryParams.setApplicationName(ContentTrendsService.CONTENT_TRENDS_AUDIT_APPLICATION);

        this.auditService.auditQuery(finder, queryParams, 1);
        
        final NodeScores historicScore = finder.getNodeScore();
        final Pair<NodeScores, Long> scoreAndDate;
        if (historicScore != null)
        {
            scoreAndDate = new Pair<NodeScores, Long>(historicScore, finder.getNodeScoreDateMillis());
        }
        else
        {
            scoreAndDate = null;
        }

        return scoreAndDate;
    }

    private Pair<NodeScores, Long> findHistoricScoreWithBackstep(final NodeRef nodeRef, final long now)
    {
        NodeScores historicScore = null;
        final long historicBaseTime = now - BACKSTEP_FOR_HISTORIC_SCORE;

        // look for historic score in time frame one day ago +- 12 hours
        final ClosestHistoricScoreFinder finder = new ClosestHistoricScoreFinder(nodeRef, historicBaseTime);
        for (int percentLookupWindow = 10; historicScore == null && percentLookupWindow <= 100; percentLookupWindow += 10)
        {
            final long lookupWindow = (percentLookupWindow * HISTORIC_LOOKUP_WINDOW) / 100;
            final long timeFrom = historicBaseTime - lookupWindow;
            final long timeTo = historicBaseTime + lookupWindow;

            final AuditQueryParameters queryParams = new AuditQueryParameters();
            queryParams.setFromTime(timeFrom);
            queryParams.setToTime(timeTo);
            queryParams.setForward(true);
            queryParams.addSearchKey(ContentTrendsService.HISTORIC_NODE_REF_PATH, nodeRef);
            queryParams.setApplicationName(ContentTrendsService.CONTENT_TRENDS_AUDIT_APPLICATION);

            this.auditService.auditQuery(finder, queryParams, Integer.MAX_VALUE);

            historicScore = finder.getNodeScore();
        }

        final Pair<NodeScores, Long> scoreAndDate;
        if (historicScore != null)
        {
            scoreAndDate = new Pair<NodeScores, Long>(historicScore, finder.getNodeScoreDateMillis());
        }
        else
        {
            scoreAndDate = null;
        }

        return scoreAndDate;
    }

    protected void saveHistoricScore(final NodeRef nodeRef, final NodeScores nodeScore)
    {
        final Map<String, Serializable> auditValues = new HashMap<String, Serializable>();
        auditValues.put("nodeRef", nodeRef);

        for (final NodeScoreType scoreType : NodeScoreType.values())
        {
            final double score = nodeScore.getScore(scoreType);
            auditValues.put(scoreType.getScoreProperty().getLocalName(), score);
        }

        this.auditComponent.recordAuditValues("/ContentTrends/Historic", auditValues);
    }

    /**
     * Update any content that was previously scored.
     * 
     * @return
     */
    protected Collection<NodeScores> calculatePreviouslyScoredContentScores(final long fromTime)
    {
        final Collection<NodeScores> scores = new HashSet<NodeScores>();

        final String query = "ASPECT:\"ct:scored\" AND TYPE:\"cm:content\"";
        final SearchParameters searchParameters = new SearchParameters();
        searchParameters.setQuery(query);
        searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        searchParameters.setLimitBy(LimitBy.FINAL_SIZE);
        searchParameters.setMaxItems(SEARCH_BATCH_SIZE);
        searchParameters.addStore(this.storeToProcess);
        searchParameters.addSort("cm:name", true);

        int lastResultSize = 1;
        int iterations = 0;

        while (lastResultSize > 0)
        {
            searchParameters.setSkipCount(iterations * SEARCH_BATCH_SIZE);
            final ResultSet resultSet = this.searchService.query(searchParameters);
            try
            {
                lastResultSize = resultSet.length();
                iterations++;
                for (final ResultSetRow row : resultSet)
                {
                    final NodeRef noderef = row.getNodeRef();
                    final NodeScores nodeScores = scoreNode(noderef, fromTime);
                    scores.add(nodeScores);
                }
            }
            finally
            {
                resultSet.close();
            }
        }

        return scores;
    }

    protected Collection<NodeScores> calculateUnscoredContentScores(final long fromTime, final Collection<Long> excludeAuditEntryIds)
    {
        /*
         * In order to traverse all audit entries and reuse our scodeNode method without loading all audit values in-bulk / inefficiently
         * (e.g. including those already processed for previously scored content), we use a slightly staged / layered approach here:
         * 
         * 1. layer (EntryScanningScorer) scans audit entry IDs for unprocessed audit entries in the relevant time range
         * 
         * 2. layer (getNodeRefFromAuditEntry) obtains the affected nodeRef from a specific audit entry that has not yet been processed
         * 
         * 3. layer (scoreNode) actually scores the affected nodeRef
         */

        final AuditQueryParameters queryParams = new AuditQueryParameters();
        queryParams.setFromTime(fromTime);
        queryParams.setForward(true);
        queryParams.setApplicationName(ContentTrendsService.CONTENT_TRENDS_AUDIT_APPLICATION);

        final EntryScanningScorer scanningScorer = new EntryScanningScorer(fromTime, excludeAuditEntryIds, this.storeToProcess);
        this.auditService.auditQuery(scanningScorer, queryParams, -1);

        final Collection<NodeScores> nodeScores = scanningScorer.getNodeScores();
        return nodeScores;
    }

    protected NodeScores scoreNode(final NodeRef nodeToScore, final long fromTime)
    {
        final UserNodeEventsByDayCollector collector = new UserNodeEventsByDayCollector(fromTime);
        final AuditQueryParameters queryParams = new AuditQueryParameters();
        queryParams.setFromTime(fromTime);
        queryParams.setForward(true);
        queryParams.setApplicationName(ContentTrendsService.CONTENT_TRENDS_AUDIT_APPLICATION);
        queryParams.addSearchKey(AGGREGATED_NODE_REF_PATH, nodeToScore);

        this.auditService.auditQuery(collector, queryParams, Integer.MAX_VALUE);

        final NodeScores nodeScores = new NodeScores(nodeToScore, collector.getHandledAuditEntryIds());

        this.scoringStrategy.score(nodeScores, collector.getCollectedUserNodeEventsByDay());

        return nodeScores;
    }

    /**
     * Checks wether the provided node has a tag event in its entire content trends history. This method is usually used to determine wether
     * a checkin of a working copy also contains an update to the tags of a node that should be reflected in the original node event counts.
     * 
     * @param node
     * @return
     */
    protected boolean hasTagEvent(final NodeRef node)
    {
        final TagEventFinder finder = new TagEventFinder();
        final AuditQueryParameters queryParams = new AuditQueryParameters();
        queryParams.setForward(true);
        queryParams.setApplicationName(ContentTrendsService.CONTENT_TRENDS_AUDIT_APPLICATION);
        queryParams.addSearchKey(AGGREGATED_NODE_REF_PATH, node);

        this.auditService.auditQuery(finder, queryParams, -1);

        return finder.isTageEventFound();
    }

    protected NodeRef getNodeRefFromAuditEntry(final Long auditEntryId)
    {
        final AuditEntryNodeRefExtractor extractor = new AuditEntryNodeRefExtractor();
        final AuditQueryParameters queryParams = new AuditQueryParameters();
        queryParams.setForward(true);
        queryParams.setApplicationName(ContentTrendsService.CONTENT_TRENDS_AUDIT_APPLICATION);
        queryParams.setFromId(auditEntryId);
        // this is exclusive, not inclusive, so we rely on maxResults in auditQuery
        // queryParams.setToId(auditEntryId);

        this.auditComponent.auditQuery(extractor, queryParams, 1);

        final NodeRef nodeRef = extractor.getNodeRef();

        final NodeRef result = (nodeRef != null && this.nodeService.exists(nodeRef)) ? nodeRef : null;
        return result;
    }

    protected class AuditEntryNodeRefExtractor implements AuditQueryCallback
    {
        private NodeRef nodeRef;

        protected NodeRef getNodeRef()
        {
            return this.nodeRef;
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
            this.nodeRef = (NodeRef) values.get(AGGREGATED_NODE_REF_PATH);
            return false;
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

    protected class EntryScanningScorer implements AuditQueryCallback
    {
        private final Collection<Long> auditEntryIdsToExclude;
        private final long fromTime;
        private final Collection<NodeScores> nodeScores = new HashSet<NodeScores>();
        private final StoreRef storeToProcess;

        protected EntryScanningScorer(final long fromTime, final Collection<Long> auditEntryIdsToExclude, final StoreRef storeToProcess)
        {
            this.auditEntryIdsToExclude = new HashSet<Long>(auditEntryIdsToExclude);
            this.fromTime = fromTime;
            this.storeToProcess = storeToProcess;
        }

        protected Collection<NodeScores> getNodeScores()
        {
            return Collections.unmodifiableCollection(this.nodeScores);
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
            if (!this.auditEntryIdsToExclude.contains(entryId))
            {
                final NodeRef nodeRef = ContentTrendsScoreUpdater.this.getNodeRefFromAuditEntry(entryId);
                if (nodeRef != null && this.storeToProcess.equals(nodeRef.getStoreRef()))
                {
                    final NodeScores nodeScores = ContentTrendsScoreUpdater.this.scoreNode(nodeRef, this.fromTime);
                    this.nodeScores.add(nodeScores);
                    this.auditEntryIdsToExclude.addAll(nodeScores.getAuditEntryIds());
                }
            }

            // continue
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

    protected class UserNodeEventsByDayCollector implements AuditQueryCallback
    {

        private final String iso8601FromHourFragment;
        private final Map<String, Map<String, UserNodeEvents>> userNodeEventsByDay = new HashMap<String, Map<String, UserNodeEvents>>();
        private final Collection<Long> auditEntryIds = new HashSet<Long>();

        protected UserNodeEventsByDayCollector(final long fromTime)
        {
            final Date fromDate = new Date(fromTime);
            final String iso8601DateTime = ISO8601DateFormat.format(fromDate);

            // extract the date part
            this.iso8601FromHourFragment = iso8601DateTime.substring(0, 13);
        }

        protected Collection<Long> getHandledAuditEntryIds()
        {
            return Collections.unmodifiableCollection(this.auditEntryIds);
        }

        protected Map<String, Map<String, UserNodeEvents>> getCollectedUserNodeEventsByDay()
        {
            return Collections.unmodifiableMap(this.userNodeEventsByDay);
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
            this.auditEntryIds.add(entryId);
            final String realUser = (String) values.get(AGGREGATED_USER_PATH);
            final String iso8601DateForHour = (String) values.get(AGGREGATED_HOUR_PATH);

            @SuppressWarnings("unchecked")
            final Collection<NodeRef> checkedInWorkingCopies = (Collection<NodeRef>) values.get(AGGREGATED_CHECKED_IN_WORKING_COPIES_PATH);

            // check base hour of aggregated record
            if (realUser != null && iso8601DateForHour != null && iso8601DateForHour.compareTo(this.iso8601FromHourFragment) >= 0)
            {
                final String dateFragment = iso8601DateForHour.substring(0, 10);

                Map<String, UserNodeEvents> nodeEventsByUser = this.userNodeEventsByDay.get(dateFragment);
                if (nodeEventsByUser == null)
                {
                    nodeEventsByUser = new HashMap<String, UserNodeEvents>();
                    this.userNodeEventsByDay.put(dateFragment, nodeEventsByUser);
                }

                UserNodeEvents nodeEvents = nodeEventsByUser.get(realUser);
                if (nodeEvents == null)
                {
                    nodeEvents = new UserNodeEvents();
                    nodeEventsByUser.put(realUser, nodeEvents);
                }

                for (final ContentTrendsEventType eventType : ContentTrendsEventType.values())
                {
                    final String valueKey = MessageFormat.format("{0}/{1}", AGGREGATED_CONTENT_TRENDS_ROOT_PATH, eventType.name());
                    final Number amount = (Number) values.get(valueKey);

                    if (amount != null)
                    {
                        switch (eventType)
                        {
                        case COCI:
                            nodeEvents.addEvents(eventType, amount.intValue(), checkedInWorkingCopies);

                            if (checkedInWorkingCopies != null)
                            {
                                int additionalTags = 0;
                                for (final NodeRef workingCopy : checkedInWorkingCopies)
                                {
                                    if (ContentTrendsScoreUpdater.this.hasTagEvent(workingCopy))
                                    {
                                        additionalTags++;
                                    }
                                }
                                nodeEvents.addEvents(ContentTrendsEventType.TAG, additionalTags, null);
                            }

                            break;
                        default:
                            nodeEvents.addEvents(eventType, amount.intValue(), null);
                        }
                    }
                }
            }

            // continue
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

    protected class FirstHistoricScoreFinder implements AuditQueryCallback
    {
        private final NodeRef nodeRef;
        
        private NodeScores nodeScore = null;
        private long nodeScoreDate = 0;

        protected FirstHistoricScoreFinder(final NodeRef nodeRef)
        {
            this.nodeRef = nodeRef;
        }

        protected NodeScores getNodeScore()
        {
            return this.nodeScore;
        }
        
        protected long getNodeScoreDateMillis()
        {
            return this.nodeScoreDate;
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
            this.nodeScoreDate = time;
            this.nodeScore = new NodeScores(this.nodeRef, Collections.<Long> emptySet());

            for (final NodeScoreType scoreType : NodeScoreType.values())
            {
                final double score = (Double) values.get(ContentTrendsService.HISTORIC_SCORE_PATHS.get(scoreType));
                this.nodeScore.addScore(scoreType, score);
            }

            return false;
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

    protected class ClosestHistoricScoreFinder implements AuditQueryCallback
    {

        private final long baseDate;
        private final NodeRef nodeRef;

        private NodeScores nodeScore = null;
        private long nodeScoreDate = 0;

        protected ClosestHistoricScoreFinder(final NodeRef nodeRef, final long baseDate)
        {
            this.nodeRef = nodeRef;
            this.baseDate = baseDate;
        }

        protected NodeScores getNodeScore()
        {
            return this.nodeScore;
        }

        protected long getNodeScoreDateMillis()
        {
            return this.nodeScoreDate;
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

            if (this.nodeScoreDate == 0 || (Math.abs(this.baseDate - time) < Math.abs(this.baseDate - this.nodeScoreDate)))
            {
                this.nodeScore = new NodeScores(this.nodeRef, Collections.<Long> emptySet());
                this.nodeScoreDate = time;

                for (final NodeScoreType scoreType : NodeScoreType.values())
                {
                    final double score = (Double) values.get(ContentTrendsService.HISTORIC_SCORE_PATHS.get(scoreType));
                    this.nodeScore.addScore(scoreType, score);
                }
            }

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
}
