/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.scoring;

import java.util.Map;
import java.util.Map.Entry;

import com.prodyna.alfresco.contenttrends.repo.module.jobs.NodeEventScoringStrategy;
import com.prodyna.alfresco.contenttrends.repo.module.jobs.NodeScores;
import com.prodyna.alfresco.contenttrends.repo.module.jobs.UserNodeEvents;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class LinearNodeEventScoringStrategy implements NodeEventScoringStrategy
{

    private int commentWeight = 1;
    private int ratingWeight = 1;
    private int viewWeight = 1;
    private int editWeight = 1;
    private int tagWeight = 1;

    private int cociEditWeight = 1;
    private int copyEditWeight = 1;
    private int editEditWeight = 1;
    private int editContentEditWeight = 1;

    private int viewViewWeight = 1;
    private int downloadViewWeight = 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public void score(NodeScores nodeScores, Map<String, Map<String, UserNodeEvents>> eventsByUserAndDay)
    {
        // we don't average out over sum of weights, since this will result in (near) zero scores for low partial scores
        // final int sumEditWeights = this.cociEditWeight + this.copyEditWeight + this.editContentEditWeight + this.editEditWeight;
        // final int sumViewWeights = this.viewViewWeight + this.downloadViewWeight;
        // final int sumTotalWeights = this.viewWeight + this.tagWeight + this.ratingWeight + this.commentWeight + this.editWeight;

        for (final Entry<String, Map<String, UserNodeEvents>> dayEventEntry : eventsByUserAndDay.entrySet())
        {
            for (final UserNodeEvents userNodeEvents : dayEventEntry.getValue().values())
            {

                nodeScores.addCommentScore(userNodeEvents.getComments());
                nodeScores.addRatingScore(userNodeEvents.getRatings());
                nodeScores.addTagScore(userNodeEvents.getTags());

                final double viewScore = (this.viewViewWeight * userNodeEvents.getViews() + this.downloadViewWeight
                        * userNodeEvents.getDownloads());
                nodeScores.addViewScore(viewScore);

                final int cociUserDayCount = userNodeEvents.getCheckins()
                        + Math.max(0,
                                userNodeEvents.getCheckouts() - userNodeEvents.getCheckins() - userNodeEvents.getCheckoutCancellations());
                final double editWeighedScore = this.cociEditWeight * cociUserDayCount + this.editEditWeight * userNodeEvents.getEdits()
                        + this.editContentEditWeight * userNodeEvents.getContentEdits() + this.copyEditWeight * userNodeEvents.getCopies();
                nodeScores.addEditScore((editWeighedScore));
            }
        }

        final double totalWeighedScore = this.commentWeight * nodeScores.getCommentScore() + this.ratingWeight
                * nodeScores.getRatingScore() + this.viewWeight * nodeScores.getViewScore() + this.tagWeight * nodeScores.getTagScore()
                + this.editWeight * nodeScores.getEditScore();

        nodeScores.addTotalScore(totalWeighedScore);
    }

    /**
     * @param commentWeight
     *            the commentWeight to set
     */
    public void setCommentWeight(int commentWeight)
    {
        this.commentWeight = commentWeight;
    }

    /**
     * @param ratingWeight
     *            the ratingWeight to set
     */
    public void setRatingWeight(int ratingWeight)
    {
        this.ratingWeight = ratingWeight;
    }

    /**
     * @param viewWeight
     *            the viewWeight to set
     */
    public void setViewWeight(int viewWeight)
    {
        this.viewWeight = viewWeight;
    }

    /**
     * @param editWeight
     *            the editWeight to set
     */
    public void setEditWeight(int editWeight)
    {
        this.editWeight = editWeight;
    }

    /**
     * @param tagWeight
     *            the tagWeight to set
     */
    public void setTagWeight(int tagWeight)
    {
        this.tagWeight = tagWeight;
    }

    /**
     * @param cociEditWeight
     *            the cociEditWeight to set
     */
    public void setCociEditWeight(int cociEditWeight)
    {
        this.cociEditWeight = cociEditWeight;
    }

    /**
     * @param copyEditWeight
     *            the copyEditWeight to set
     */
    public void setCopyEditWeight(int copyEditWeight)
    {
        this.copyEditWeight = copyEditWeight;
    }

    /**
     * @param editContentEditWeight
     *            the editContentEditWeight to set
     */
    public void setEditContentEditWeight(int editContentEditWeight)
    {
        this.editContentEditWeight = editContentEditWeight;
    }

    /**
     * @param editEditWeight
     *            the editEditWeight to set
     */
    public void setEditEditWeight(int editEditWeight)
    {
        this.editEditWeight = editEditWeight;
    }

    /**
     * @param viewViewWeight
     *            the viewViewWeight to set
     */
    public void setViewViewWeight(int viewViewWeight)
    {
        this.viewViewWeight = viewViewWeight;
    }

    /**
     * @param downloadViewWeight
     *            the downloadViewWeight to set
     */
    public void setDownloadViewWeight(int downloadViewWeight)
    {
        this.downloadViewWeight = downloadViewWeight;
    }

}
