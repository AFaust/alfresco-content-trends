/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import org.alfresco.error.AlfrescoRuntimeException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ContentTrendsCleanerJob implements Job
{

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        final JobDataMap jobData = context.getJobDetail().getJobDataMap();
        final Object potentialCleaner = jobData.get("cleaner");
        if (potentialCleaner instanceof ContentTrendsCleaner)
        {
            ((ContentTrendsCleaner) potentialCleaner).cleanEvents();
        }
        else
        {
            throw new AlfrescoRuntimeException("ContentTrendsCleanerJobData data must contain valid 'cleaner' reference");
        }
    }

}
