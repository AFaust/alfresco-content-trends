/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ContentTrendsServiceImpl implements ContentTrendsService, InitializingBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTrendsServiceImpl.class);

    protected static Map<NodeScoreType, String> initializeHistoricScorePaths()
    {
        final Map<NodeScoreType, String> map = new EnumMap<NodeScoreType, String>(NodeScoreType.class);
        for (final NodeScoreType scoreType : NodeScoreType.values())
        {
            final String scorePath = HISTORIC_TRENDS_AUDIT_PATH + "/" + scoreType.getScoreProperty().getLocalName();
            map.put(scoreType, scorePath);
        }
        return map;
    }

    protected int defaultNumberOfDaysBack = ContentTrendsService.DEFAULT_NUMBER_OF_DAYS_BACK;

    protected AuditService auditService;

    public void setAuditService(final AuditService auditService)
    {
        this.auditService = auditService;
    }

    public void setDefaultNumberOfDaysBack(final int defaultNumberOfDaysBack)
    {
        if (defaultNumberOfDaysBack < 0)
        {
            throw new IllegalArgumentException("defaultNumberOfDaysBack must be a non-negative integer");
        }

        this.defaultNumberOfDaysBack = defaultNumberOfDaysBack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "auditService", this.auditService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatedNodeScores> getScoreHistory(final NodeRef nodeRef)
    {
        return this.getScoreHistory(nodeRef, this.defaultNumberOfDaysBack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatedNodeScores> getScoreHistory(final NodeRef nodeRef, final int numberOfDaysBack)
    {
        ParameterCheck.mandatory("nodeRef", nodeRef);

        if (numberOfDaysBack < 0)
        {
            throw new IllegalArgumentException("numberOfDaysBack must be a non-negative integer");
        }

        final ScoreHistoryCollector collector = new ScoreHistoryCollector(nodeRef);
        final AuditQueryParameters params = new AuditQueryParameters();
        params.setForward(true);
        params.setApplicationName(CONTENT_TRENDS_AUDIT_APPLICATION);
        params.addSearchKey(HISTORIC_NODE_REF_PATH, nodeRef);

        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, -numberOfDaysBack);
        params.setFromTime(cal.getTimeInMillis());

        this.auditService.auditQuery(collector, params, -1);
        final List<DatedNodeScores> scoreHistory = collector.getNodeScores();

        // add a history for current score state
        final DatedNodeScores currentScores = new DatedNodeScores(nodeRef, new Date());
        if (!scoreHistory.isEmpty())
        {
            // clone the last score state as the current data point state
            final DatedNodeScores lastNodeScores = scoreHistory.get(scoreHistory.size() - 1);
            for (final NodeScoreType scoreType : NodeScoreType.values())
            {
                currentScores.setScore(scoreType, lastNodeScores.getScore(scoreType));
            }
        }
        scoreHistory.add(currentScores);

        return scoreHistory;
    }

    protected static class ScoreHistoryCollector implements AuditQueryCallback
    {
        private final NodeRef nodeRef;
        private final List<DatedNodeScores> nodeScores = new ArrayList<DatedNodeScores>();

        protected ScoreHistoryCollector(final NodeRef nodeRef)
        {
            this.nodeRef = nodeRef;
        }

        protected List<DatedNodeScores> getNodeScores()
        {
            return new ArrayList<DatedNodeScores>(this.nodeScores);
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
            final DatedNodeScores nodeScore = new DatedNodeScores(this.nodeRef, new Date(time));

            for (final NodeScoreType scoreType : NodeScoreType.values())
            {
                final double score = (Double) values.get(HISTORIC_SCORE_PATHS.get(scoreType));
                nodeScore.setScore(scoreType, (int) Math.round(score));
            }

            this.nodeScores.add(nodeScore);

            return true;
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean handleAuditEntryError(final Long entryId, final String errorMsg, final Throwable error)
        {
            LOGGER.error("Error handling audit event () while collecting score history for (): ()", new Object[] { entryId, this.nodeRef,
                    errorMsg });

            // continue
            return true;
        }
    }
}
