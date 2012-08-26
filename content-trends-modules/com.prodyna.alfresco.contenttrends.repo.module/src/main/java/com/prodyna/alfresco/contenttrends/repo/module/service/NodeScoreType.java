/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.service;

import org.alfresco.service.namespace.QName;

import com.prodyna.alfresco.contenttrends.repo.module.model.ContentTrendsModel;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public enum NodeScoreType
{

    TOTAL_SCORE(ContentTrendsModel.PROP_TOTAL_SCORE, ContentTrendsModel.PROP_TOTAL_SCORE_CHANGE),

    VIEW_SCORE(ContentTrendsModel.PROP_VIEW_SCORE, ContentTrendsModel.PROP_VIEW_SCORE_CHANGE),

    TAG_SCORE(ContentTrendsModel.PROP_TAG_SCORE, ContentTrendsModel.PROP_TAG_SCORE_CHANGE),

    RATING_SCORE(ContentTrendsModel.PROP_RATING_SCORE, ContentTrendsModel.PROP_RATING_SCORE_CHANGE),

    COMMENT_SCORE(ContentTrendsModel.PROP_COMMENT_SCORE, ContentTrendsModel.PROP_COMMENT_SCORE_CHANGE),

    EDIT_SCORE(ContentTrendsModel.PROP_EDIT_SCORE, ContentTrendsModel.PROP_EDIT_SCORE_CHANGE);

    private final QName scoreProperty;
    private final QName scoreChangeProperty;

    private NodeScoreType(final QName scoreProperty, final QName scoreChangeProperty)
    {
        this.scoreProperty = scoreProperty;
        this.scoreChangeProperty = scoreChangeProperty;
    }

    /**
     * @return the scoreProperty
     */
    public QName getScoreProperty()
    {
        return scoreProperty;
    }

    /**
     * @return the scoreChangeProperty
     */
    public QName getScoreChangeProperty()
    {
        return scoreChangeProperty;
    }

}
