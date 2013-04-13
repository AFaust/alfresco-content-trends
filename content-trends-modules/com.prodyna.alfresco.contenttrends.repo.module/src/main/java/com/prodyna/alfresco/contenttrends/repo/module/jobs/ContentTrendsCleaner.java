/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prodyna.alfresco.contenttrends.repo.module.model.ContentTrendsModel;
import com.prodyna.alfresco.contenttrends.repo.module.service.ContentTrendsService;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ContentTrendsCleaner extends AbstractContentTrendsProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTrendsCleaner.class);

    protected static final String AGGREGATED_CONTENT_TRENDS_ROOT_PATH = "/" + ContentTrendsService.CONTENT_TRENDS_AUDIT_APPLICATION
            + "/Aggregated";
    protected static final String AGGREGATED_NODE_REF_PATH = AGGREGATED_CONTENT_TRENDS_ROOT_PATH + "/nodeRef";

    protected static final QName LOCK_QNAME = QName.createQName(ContentTrendsModel.NAMESPACE_URI, "ContentTrendsCleaner");
    protected static final long LOCK_TTL = 1000 * 30;

    private int historicScoreMaxAgeInDays = ContentTrendsService.DEFAULT_NUMBER_OF_DAYS_BACK;
    private int aggregatedEventsMaxAgeInDays = ContentTrendsScoreUpdater.DEFAULT_SCORING_WINDOW_SIZE_IN_DAYS;

    public final void cleanEvents()
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
        clearEvents(this.aggregatedEventsMaxAgeInDays, AGGREGATED_NODE_REF_PATH);
        clearEvents(this.historicScoreMaxAgeInDays, ContentTrendsService.HISTORIC_NODE_REF_PATH);
    }

    private void clearEvents(final int maxAge, final String checkPath)
    {
        final Calendar cal = Calendar.getInstance();

        // go back to the beginning of the hour interval which is still inside the maxAge window
        cal.add(Calendar.DATE, -maxAge);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // now go back to the last millisecond of the "tooOld" window
        cal.add(Calendar.MILLISECOND, -1);

        final long deleteToTime = cal.getTimeInMillis();

        // we can't do a simple clear since this might also clear audit entries from the other part of our application
        final AuditEntryIdCollector entryCollector = new TargetedAuditEntryIdCollector(checkPath);

        final AuditQueryParameters queryParams = new AuditQueryParameters();
        queryParams.setToTime(deleteToTime);
        queryParams.setApplicationName(ContentTrendsService.CONTENT_TRENDS_AUDIT_APPLICATION);

        this.auditService.auditQuery(entryCollector, queryParams, Integer.MAX_VALUE);

        final List<Long> entryIdsForDeletion = entryCollector.getAuditEntryIds();
        LOGGER.debug("Cleaning {} audit entries from ContentTrends", entryIdsForDeletion.size());
        this.auditService.clearAudit(entryIdsForDeletion);
    }

    private static class TargetedAuditEntryIdCollector extends AuditEntryIdCollector
    {

        private final String checkPath;

        protected TargetedAuditEntryIdCollector(final String checkPath)
        {
            this.checkPath = checkPath;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean valuesRequired()
        {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean handleAuditEntry(Long entryId, String applicationName, String user, long time, Map<String, Serializable> values)
        {
            if (values != null && values.containsKey(checkPath))
            {
                return super.handleAuditEntry(entryId, applicationName, user, time, values);
            }
            else
            {
                return true;
            }
        }

    }

    /**
     * @param historicScoreMaxAgeInDays
     *            the historicScoreMaxAgeInDays to set
     */
    public void setHistoricScoreMaxAgeInDays(int historicScoreMaxAgeInDays)
    {
        if (historicScoreMaxAgeInDays > 0)
        {
            this.historicScoreMaxAgeInDays = historicScoreMaxAgeInDays;
        }
    }

    /**
     * @param aggregatedEventsMaxAgeInDays
     *            the aggregatedEventsMaxAgeInDays to set
     */
    public void setAggregatedEventsMaxAgeInDays(int aggregatedEventsMaxAgeInDays)
    {
        if (aggregatedEventsMaxAgeInDays > 0)
        {
            this.aggregatedEventsMaxAgeInDays = aggregatedEventsMaxAgeInDays;
        }
    }

}
