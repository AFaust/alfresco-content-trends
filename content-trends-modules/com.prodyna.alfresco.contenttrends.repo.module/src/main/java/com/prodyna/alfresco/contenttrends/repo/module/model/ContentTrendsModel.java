/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ContentTrendsModel
{

    static final String NAMESPACE_URI = "http://www.prodyna.com/model/contenttrends/0.1";
    static final String NAMESPACE_PREFIX = "ct";

    static final QName ASPECT_SCORED = QName.createQName(NAMESPACE_URI, "scored");
    static final QName PROP_TOTAL_SCORE = QName.createQName(NAMESPACE_URI, "totalScore");
    static final QName PROP_VIEW_SCORE = QName.createQName(NAMESPACE_URI, "viewScore");
    static final QName PROP_TAG_SCORE = QName.createQName(NAMESPACE_URI, "tagScore");
    static final QName PROP_COMMENT_SCORE = QName.createQName(NAMESPACE_URI, "commentScore");
    static final QName PROP_EDIT_SCORE = QName.createQName(NAMESPACE_URI, "editScore");
    static final QName PROP_RATING_SCORE = QName.createQName(NAMESPACE_URI, "ratingScore");
    
    static final QName ASPECT_SCORED_CHANGE = QName.createQName(NAMESPACE_URI, "scoredChange");
    static final QName PROP_TOTAL_SCORE_CHANGE = QName.createQName(NAMESPACE_URI, "totalScoreChange");
    static final QName PROP_VIEW_SCORE_CHANGE = QName.createQName(NAMESPACE_URI, "viewScoreChange");
    static final QName PROP_TAG_SCORE_CHANGE = QName.createQName(NAMESPACE_URI, "tagScoreChange");
    static final QName PROP_COMMENT_SCORE_CHANGE = QName.createQName(NAMESPACE_URI, "commentScoreChange");
    static final QName PROP_EDIT_SCORE_CHANGE = QName.createQName(NAMESPACE_URI, "editScoreChange");
    static final QName PROP_RATING_SCORE_CHANGE = QName.createQName(NAMESPACE_URI, "ratingScoreChange");
}
