package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import java.util.Collection;
import java.util.Collections;

import org.alfresco.service.cmr.repository.NodeRef;

import com.prodyna.alfresco.contenttrends.repo.module.service.NodeScoreType;

/**
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class NodeScores
{
    private final NodeRef nodeRef;
    private final Collection<Long> auditEntryIds;

    private double tagScore = 0;
    private double commentScore = 0;
    private double editScore = 0;
    private double viewScore = 0;
    private double ratingScore = 0;
    private double totalScore = 0;

    public NodeScores(final NodeRef nodeRef, final Collection<Long> auditEntryIds)
    {
        this.nodeRef = nodeRef;
        this.auditEntryIds = auditEntryIds;
    }

    public Collection<Long> getAuditEntryIds()
    {
        return Collections.unmodifiableCollection(this.auditEntryIds);
    }

    public NodeRef getNodeRef()
    {
        return this.nodeRef;
    }

    public double getScore(final NodeScoreType scoreType)
    {
        final double result;

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

    public void addScore(final NodeScoreType scoreType, final double scoreToAdd)
    {
        if (scoreToAdd > 0)
        {
            switch (scoreType)
            {
            case TOTAL_SCORE:
                this.totalScore += scoreToAdd;
                break;
            case VIEW_SCORE:
                this.viewScore += scoreToAdd;
                break;
            case TAG_SCORE:
                this.tagScore += scoreToAdd;
                break;
            case RATING_SCORE:
                this.ratingScore += scoreToAdd;
                break;
            case COMMENT_SCORE:
                this.commentScore += scoreToAdd;
                break;
            case EDIT_SCORE:
                this.editScore += scoreToAdd;
                break;
            }
        }
    }

    /**
     * @deprecated Use {@link #getScore(NodeScoreType) getScore}
     */
    @Deprecated
    public double getTotalScore()
    {
        return this.totalScore;
    }

    /**
     * @deprecated Use {@link #getScore(NodeScoreType) getScore}
     */
    @Deprecated
    public double getTagScore()
    {
        return this.tagScore;
    }

    /**
     * @deprecated Use {@link #getScore(NodeScoreType) getScore}
     */
    @Deprecated
    public double getCommentScore()
    {
        return this.commentScore;
    }

    /**
     * @deprecated Use {@link #getScore(NodeScoreType) getScore}
     */
    @Deprecated
    public double getEditScore()
    {
        return this.editScore;
    }

    /**
     * @deprecated Use {@link #getScore(NodeScoreType) getScore}
     */
    @Deprecated
    public double getViewScore()
    {
        return this.viewScore;
    }

    /**
     * @deprecated Use {@link #getScore(NodeScoreType) getScore}
     */
    @Deprecated
    public double getRatingScore()
    {
        return this.ratingScore;
    }

    /**
     * @deprecated Use {@link #addScore(NodeScoreType, double) addScore}
     */
    @Deprecated
    public void addTotalScore(final double scoreToAdd)
    {
        if (scoreToAdd > 0)
        {
            this.totalScore += scoreToAdd;
        }
    }

    /**
     * @deprecated Use {@link #addScore(NodeScoreType, double) addScore}
     */
    @Deprecated
    public void addTagScore(final double scoreToAdd)
    {
        if (scoreToAdd > 0)
        {
            this.tagScore += scoreToAdd;
        }
    }

    /**
     * @deprecated Use {@link #addScore(NodeScoreType, double) addScore}
     */
    @Deprecated
    public void addCommentScore(final double scoreToAdd)
    {
        if (scoreToAdd > 0)
        {
            this.commentScore += scoreToAdd;
        }
    }

    /**
     * @deprecated Use {@link #addScore(NodeScoreType, double) addScore}
     */
    @Deprecated
    public void addEditScore(final double scoreToAdd)
    {
        if (scoreToAdd > 0)
        {
            this.editScore += scoreToAdd;
        }
    }

    /**
     * @deprecated Use {@link #addScore(NodeScoreType, double) addScore}
     */
    @Deprecated
    public void addViewScore(final double scoreToAdd)
    {
        if (scoreToAdd > 0)
        {
            this.viewScore += scoreToAdd;
        }
    }

    /**
     * @deprecated Use {@link #addScore(NodeScoreType, double) addScore}
     */
    @Deprecated
    public void addRatingScore(final double scoreToAdd)
    {
        if (scoreToAdd > 0)
        {
            this.ratingScore += scoreToAdd;
        }
    }
}