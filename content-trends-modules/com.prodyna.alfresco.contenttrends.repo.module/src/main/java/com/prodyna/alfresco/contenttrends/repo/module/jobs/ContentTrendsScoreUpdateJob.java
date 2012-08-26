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
public class ContentTrendsScoreUpdateJob implements Job
{

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        final JobDataMap jobData = context.getJobDetail().getJobDataMap();
        final Object potentialUpdater = jobData.get("updater");
        if (potentialUpdater instanceof ContentTrendsScoreUpdater)
        {
            ((ContentTrendsScoreUpdater) potentialUpdater).updateScores();
        }
        else
        {
            throw new AlfrescoRuntimeException("ContentTrendsUpdateJobData data must contain valid 'updater' reference");
        }
    }

}
