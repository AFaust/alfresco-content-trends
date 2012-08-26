/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.service;

import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class DatedNodeScores
{
    private final NodeRef nodeRef;
    private final Date date;

    private int totalScore;
    private int viewScore;
    private int tagScore;
    private int ratingScore;
    private int commentScore;
    private int editScore;

    protected DatedNodeScores(final NodeRef nodeRef, final Date date)
    {
        this.nodeRef = nodeRef;
        this.date = date;
    }

    protected void setScore(final NodeScoreType scoreType, final int value)
    {
        switch (scoreType)
        {
        case TOTAL_SCORE:
            this.totalScore = value;
            break;
        case VIEW_SCORE:
            this.viewScore = value;
            break;
        case TAG_SCORE:
            this.tagScore = value;
            break;
        case RATING_SCORE:
            this.ratingScore = value;
            break;
        case COMMENT_SCORE:
            this.commentScore = value;
            break;
        case EDIT_SCORE:
            this.editScore = value;
            break;
        }
    }

    /**
     * Retrieve the score value for the score type
     * 
     * @param scoreType
     *            the type of score to retrieve
     * @return the score value at the date and for the nodeRef of this instance
     */
    public int getScore(final NodeScoreType scoreType)
    {
        final int result;

        switch (scoreType)
        {
        case TOTAL_SCORE:
            result = this.totalScore;
            break;
        case VIEW_SCORE:
            result = this.viewScore;
            break;
        case TAG_SCORE:
            result = this.tagScore;
            break;
        case RATING_SCORE:
            result = this.ratingScore;
            break;
        case COMMENT_SCORE:
            result = this.commentScore;
            break;
        case EDIT_SCORE:
            result = this.editScore;
            break;
        default:
            // should never occur
            result = 0;
        }

        return result;
    }

    /**
     * @return the nodeRef
     */
    public NodeRef getNodeRef()
    {
        return nodeRef;
    }

    /**
     * @return the date
     */
    public Date getDate()
    {
        return date;
    }

}
