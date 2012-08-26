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
public class ContentTrendsConsolidationJob implements Job
{

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        final JobDataMap jobData = context.getJobDetail().getJobDataMap();
        final Object potentialConsolidator = jobData.get("consolidator");
        if (potentialConsolidator instanceof ContentTrendsConsolidator)
        {
            ((ContentTrendsConsolidator) potentialConsolidator).consolidateEvents();
        }
        else
        {
            throw new AlfrescoRuntimeException("ContentTrendsConsolidationJobData data must contain valid 'consolidator' reference");
        }
    }

}
