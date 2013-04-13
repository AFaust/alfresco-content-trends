package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditEntryIdCollector implements AuditQueryCallback
{
    private final static Logger LOGGER = LoggerFactory.getLogger(AuditEntryIdCollector.class);

    private final List<Long> auditEntryIds = new ArrayList<Long>();

    protected List<Long> getAuditEntryIds()
    {
        return Collections.unmodifiableList(this.auditEntryIds);
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
        this.auditEntryIds.add(entryId);
        return true;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean handleAuditEntryError(final Long entryId, final String errorMsg, final Throwable error)
    {
        LOGGER.warn("Error handling audit event {}: {}", entryId, errorMsg);

        // continue
        return true;
    }

}