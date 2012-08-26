/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.service.Auditable;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ContentTrendsService
{
    static final int DEFAULT_NUMBER_OF_DAYS_BACK = 7;

    static final String CONTENT_TRENDS_AUDIT_APPLICATION = "ContentTrends";

    static final String HISTORIC_TRENDS_AUDIT_PATH = "/" + CONTENT_TRENDS_AUDIT_APPLICATION + "/Historic";
    static final String HISTORIC_NODE_REF_PATH = HISTORIC_TRENDS_AUDIT_PATH + "/nodeRef";

    static final Map<NodeScoreType, String> HISTORIC_SCORE_PATHS = Collections.unmodifiableMap(ContentTrendsServiceImpl
            .initializeHistoricScorePaths());

    /**
     * Retrieve the score history of the provided node for a default number of days back.
     * 
     * @param nodeRef
     *            the reference to the node to retrieve the score history of
     * @return the score history ordered by date of the score calculation
     */
    @Auditable(parameters = { "nodeRef" })
    List<DatedNodeScores> getScoreHistory(NodeRef nodeRef);

    /**
     * Retrieve the score history of the provided node for a specified number of days back.
     * 
     * @param nodeRef
     *            the reference to the node to retrieve the score history of
     * @param numberOfDaysBack
     *            the number of days to go back in history - if numberOfDaysBack is zero, this will retrieve only todays scores
     * @return the score history ordered by date of the score calculation
     */
    @Auditable(parameters = { "nodeRef", "numberOfDaysBack" })
    List<DatedNodeScores> getScoreHistory(NodeRef nodeRef, int numberOfDaysBack);
}
