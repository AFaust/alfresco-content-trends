/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;

import org.alfresco.repo.audit.generator.AbstractDataGenerator;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class SimpleEventGenerator extends AbstractDataGenerator
{

    private ContentTrendsEventType eventType;

    @Override
    public Serializable getData() throws Throwable
    {
        return this.eventType;
    }

    /**
     * @param eventType
     *            the eventType to set
     */
    public void setEventType(ContentTrendsEventType eventType)
    {
        this.eventType = eventType;
    }

}
