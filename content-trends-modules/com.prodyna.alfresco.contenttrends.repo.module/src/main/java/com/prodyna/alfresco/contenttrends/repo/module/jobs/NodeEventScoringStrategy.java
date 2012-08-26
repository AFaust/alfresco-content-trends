/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import java.util.Map;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface NodeEventScoringStrategy
{
    /**
     * Scores a node based on audited events based on a specific scoring scheme. While the available set of score values is pre-defined, a
     * scoring strategy is free to interpret the captured events in any way it sees fit.
     * 
     * @param nodeScores
     *            the container object for the standardized score values
     * @param eventsByUserAndDay
     *            the captured events by user and day - the outer map keys are ISO 8601 formatted fragments for the day the events were
     *            captured on while the inner map is keyed by a unique (and potentially not reverse-resolveable) identifier for the user the
     *            event was captured for
     */
    void score(NodeScores nodeScores, Map<String, Map<String, UserNodeEvents>> eventsByUserAndDay);
}
