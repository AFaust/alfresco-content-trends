package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagEventFinder implements AuditQueryCallback
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TagEventFinder.class);

    private boolean tagEventFound = false;

    protected boolean isTageEventFound()
    {
        return this.tagEventFound;
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
        if (values.containsKey(ContentTrendsScoreUpdater.AGGREGATED_TAG_EVENT_PATH))
        {
            final Number tagEventCount = (Number) values.get(ContentTrendsScoreUpdater.AGGREGATED_TAG_EVENT_PATH);
            this.tagEventFound = tagEventCount != null && tagEventCount.intValue() > 0;
        }

        // continue until the first event is found
        return !this.tagEventFound;
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