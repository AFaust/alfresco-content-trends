/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.scoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.util.Pair;

import com.prodyna.alfresco.contenttrends.repo.module.jobs.NodeEventScoringStrategy;
import com.prodyna.alfresco.contenttrends.repo.module.jobs.NodeScores;
import com.prodyna.alfresco.contenttrends.repo.module.jobs.UserNodeEvents;
import com.prodyna.alfresco.contenttrends.repo.module.jobs.UserNodeEvents.ScorableNodeEventType;
import com.prodyna.alfresco.contenttrends.repo.module.service.NodeScoreType;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class DiminishingReturnsScoringStrategy implements NodeEventScoringStrategy
{
    private static final double ASSUMED_MAX_EVENT_COUNT = 1000000d;

    /*
     * The value x for which pow(1 - 1 / (count / limit + 1), x) is about 0.8 (80 %) at count = limit, which equates to pow(0.5, x) and thus
     * (rougly) x = log(0.8) / log(0.5)
     */
    private static final double EXPONENT_FOR_80_PENALTY = 0.325d;

    // TODO: Should we scale the global limits based on actual user base size?
    // TODO: Check the defaults
    // TODO: better handling of configuration parameters

    // COCI is the sole exception in scoring at the moment: the limit applies to the combined events, since they complement each other
    protected double globalCociLimitFor80Penalty = 20;
    protected double userCociLimitFor80Penalty = 4;
    protected double checkoutModifier = 0.5d;
    protected double checkinModifier = 1.5d;

    protected double globalEditLimitFor80Penalty = 20;
    protected double userEditLimitFor80Penalty = 4;
    protected double editModifier = 0.75d;

    protected double globalEditContentLimitFor80Penalty = 20;
    protected double userEditContentLimitFor80Penalty = 4;
    protected double editContentModifier = 1;

    protected double globalViewLimitFor80Penalty = ASSUMED_MAX_EVENT_COUNT;
    protected double userViewLimitFor80Penalty = 5;
    protected double viewModifier = 0.125d;

    protected double globalDownloadLimitFor80Penalty = ASSUMED_MAX_EVENT_COUNT;
    protected double userDownloadLimitFor80Penalty = 2;
    protected double downloadModifier = 0.25d;

    protected double globalRatingLimitFor80Penalty = ASSUMED_MAX_EVENT_COUNT;
    protected double userRatingLimitFor80Penalty = 2;
    protected double ratingModifier = 0.25d;

    protected double globalCommentLimitFor80Penalty = ASSUMED_MAX_EVENT_COUNT;
    protected double userCommentLimitFor80Penalty = 5;
    protected double commentModifier = 0.5d;

    protected double globalTagLimitFor80Penalty = 20;
    protected double userTagLimitFor80Penalty = 4;
    protected double tagModifier = 0.75d;

    /**
     * {@inheritDoc}
     */
    @Override
    public void score(final NodeScores nodeScores, final Map<String, Map<String, UserNodeEvents>> eventsByUserAndDay)
    {
        final Map<NodeScoreType, Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>>> scoreParametersByScore = initializeScoreParameters();

        for (final NodeScoreType nodeScoreType : scoreParametersByScore.keySet())
        {
            final Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>> scoreParameters = scoreParametersByScore
                    .get(nodeScoreType);
            score(nodeScores, eventsByUserAndDay, nodeScoreType, scoreParameters);
        }
    }

    protected synchronized void scaleModifiers()
    {
        double lowestModifier = Double.MAX_VALUE;

        lowestModifier = Math.min(lowestModifier, this.viewModifier);
        lowestModifier = Math.min(lowestModifier, this.downloadModifier);
        lowestModifier = Math.min(lowestModifier, this.editModifier);
        lowestModifier = Math.min(lowestModifier, this.editContentModifier);
        lowestModifier = Math.min(lowestModifier, this.tagModifier);
        lowestModifier = Math.min(lowestModifier, this.ratingModifier);
        lowestModifier = Math.min(lowestModifier, this.commentModifier);
        lowestModifier = Math.min(lowestModifier, this.checkoutModifier);
        lowestModifier = Math.min(lowestModifier, this.checkinModifier);

        final double scaleFactor = 1 / lowestModifier;

        this.viewModifier *= scaleFactor;
        this.downloadModifier *= scaleFactor;
        this.editModifier *= scaleFactor;
        this.editContentModifier *= scaleFactor;
        this.tagModifier *= scaleFactor;
        this.ratingModifier *= scaleFactor;
        this.commentModifier *= scaleFactor;
        this.checkoutModifier *= scaleFactor;
        this.checkinModifier *= scaleFactor;
    }

    protected synchronized Map<NodeScoreType, Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>>> initializeScoreParameters()
    {
        scaleModifiers();
        final Map<NodeScoreType, Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>>> result = new EnumMap<NodeScoreType, Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>>>(
                NodeScoreType.class);

        result.put(
                NodeScoreType.COMMENT_SCORE,
                getSimpleScoreParameters(ScorableNodeEventType.COMMENTS, this.globalCommentLimitFor80Penalty,
                        this.userCommentLimitFor80Penalty, this.commentModifier));
        result.put(
                NodeScoreType.TAG_SCORE,
                getSimpleScoreParameters(ScorableNodeEventType.TAGS, this.globalTagLimitFor80Penalty, this.userTagLimitFor80Penalty,
                        this.tagModifier));
        result.put(
                NodeScoreType.RATING_SCORE,
                getSimpleScoreParameters(ScorableNodeEventType.RATINGS, this.globalRatingLimitFor80Penalty,
                        this.userRatingLimitFor80Penalty, this.ratingModifier));

        final Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>> viewScoreParameters = new EnumMap<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>>(
                ScorableNodeEventType.class);
        viewScoreParameters.putAll(getSimpleScoreParameters(ScorableNodeEventType.VIEWS, this.globalViewLimitFor80Penalty,
                this.userViewLimitFor80Penalty, this.viewModifier));
        viewScoreParameters.putAll(getSimpleScoreParameters(ScorableNodeEventType.DOWNLOADS, this.globalDownloadLimitFor80Penalty,
                this.userDownloadLimitFor80Penalty, this.downloadModifier));
        result.put(NodeScoreType.VIEW_SCORE, viewScoreParameters);

        final Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>> editScoreParameters = new EnumMap<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>>(
                ScorableNodeEventType.class);
        editScoreParameters.putAll(getSimpleScoreParameters(ScorableNodeEventType.EDITS, this.globalEditLimitFor80Penalty,
                this.userEditLimitFor80Penalty, this.editModifier));
        editScoreParameters.putAll(getSimpleScoreParameters(ScorableNodeEventType.CONTENT_EDITS, this.globalEditContentLimitFor80Penalty,
                this.userEditContentLimitFor80Penalty, this.editContentModifier));
        result.put(NodeScoreType.EDIT_SCORE, editScoreParameters);

        return result;
    }

    protected static Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>> getSimpleScoreParameters(
            final ScorableNodeEventType baseEventType, final double globalLimit, final double userLimit, final double modifier)
    {
        final Pair<Double, Double> limitPair = new Pair<Double, Double>(globalLimit, userLimit);
        final Pair<Pair<Double, Double>, Double> limitsAndModifierPair = new Pair<Pair<Double, Double>, Double>(limitPair, modifier);
        final Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>> result = Collections.singletonMap(baseEventType,
                limitsAndModifierPair);

        return result;
    }

    protected void score(final NodeScores nodeScores, final Map<String, Map<String, UserNodeEvents>> eventsByUserAndDay,
            final NodeScoreType nodeScoreType,
            final Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>> limitAndModifierByEventType)
    {
        double score = 0;
        switch (nodeScoreType)
        {
        case EDIT_SCORE:
            score += getCociScore(eventsByUserAndDay);
        default:
            score += getGenericScore(eventsByUserAndDay, limitAndModifierByEventType);
        }

        nodeScores.addScore(nodeScoreType, score);
        nodeScores.addScore(NodeScoreType.TOTAL_SCORE, score);
    }

    protected double getGenericScore(final Map<String, Map<String, UserNodeEvents>> eventsByUserAndDay,
            final Map<ScorableNodeEventType, Pair<Pair<Double, Double>, Double>> limitAndModifierByEventType)
    {
        final Map<ScorableNodeEventType, AtomicInteger> globalCounts = new EnumMap<ScorableNodeEventType, AtomicInteger>(
                ScorableNodeEventType.class);
        final Map<ScorableNodeEventType, Map<String, AtomicInteger>> userCountsByEvent = new EnumMap<ScorableNodeEventType, Map<String, AtomicInteger>>(
                ScorableNodeEventType.class);

        double score = 0;

        // we need to have a reproducible scoring order for scorable event types, otherwise we may end up with different scores on separate
        // scoring occasions for the same data set
        final List<ScorableNodeEventType> scorableEventTypes = new ArrayList<UserNodeEvents.ScorableNodeEventType>(
                limitAndModifierByEventType.keySet());
        // sorting by standard significance (ordinal of enum)
        Collections.sort(scorableEventTypes);

        for (final Entry<String, Map<String, UserNodeEvents>> outerEntry : eventsByUserAndDay.entrySet())
        {
            final Map<String, UserNodeEvents> eventsByUser = outerEntry.getValue();

            // we need to have a reproducible scoring order for user events, otherwise we may end up with different scores on separate
            // scoring occasions for the same data set
            final List<String> userKeys = new ArrayList<String>(eventsByUser.keySet());
            // plain alpha-numerical order
            Collections.sort(userKeys);

            for (final ScorableNodeEventType scorableEventType : scorableEventTypes)
            {
                AtomicInteger globalCount = globalCounts.get(scorableEventType);
                if (globalCount == null)
                {
                    globalCount = new AtomicInteger(0);
                    globalCounts.put(scorableEventType, globalCount);
                }

                Map<String, AtomicInteger> userCounts = userCountsByEvent.get(scorableEventType);
                if (userCounts == null)
                {
                    userCounts = new HashMap<String, AtomicInteger>();
                    userCountsByEvent.put(scorableEventType, userCounts);
                }

                final double eventGlobalLimitFor80Penalty = limitAndModifierByEventType.get(scorableEventType).getFirst().getFirst();
                final double eventUserLimitFor80Penalty = limitAndModifierByEventType.get(scorableEventType).getFirst().getSecond();
                final double eventModifier = limitAndModifierByEventType.get(scorableEventType).getSecond();

                for (final String userKey : userKeys)
                {

                    AtomicInteger userCount = userCounts.get(userKey);
                    if (userCount == null)
                    {
                        userCount = new AtomicInteger(0);
                        userCounts.put(userKey, userCount);
                    }

                    final UserNodeEvents userEvents = eventsByUser.get(userKey);
                    int eventAmount = userEvents.getCount(scorableEventType);

                    if (userCount.get() == 0 && eventAmount > 0)
                    {
                        // full score for first event per user
                        globalCount.incrementAndGet();
                        userCount.incrementAndGet();
                        score += eventModifier;
                        eventAmount--;
                    }

                    for (int i = 0; i < eventAmount; i++)
                    {
                        score += scoreOne(globalCount, userCount, eventGlobalLimitFor80Penalty, eventUserLimitFor80Penalty, eventModifier);
                    }
                }
            }
        }

        return score;
    }

    private double scoreOne(final AtomicInteger globalCount, final AtomicInteger userCount, final double eventGlobalLimitFor80Penalty,
            final double eventUserLimitFor80Penalty, final double eventModifier)
    {
        final double globalModifier = 1 - Math.pow(1 - 1 / (globalCount.getAndIncrement() / eventGlobalLimitFor80Penalty + 1),
                EXPONENT_FOR_80_PENALTY);
        final double userModifier = 1 - Math.pow(1 - 1 / (userCount.getAndIncrement() / eventUserLimitFor80Penalty + 1),
                EXPONENT_FOR_80_PENALTY);
        final double score = globalModifier * userModifier * eventModifier;
        return score;
    }

    protected double getCociScore(final Map<String, Map<String, UserNodeEvents>> eventsByUserAndDay)
    {
        final AtomicInteger globalCociCount = new AtomicInteger(0);
        double cociScore = 0;
        final Map<String, AtomicInteger> cociCountByUser = new HashMap<String, AtomicInteger>();

        for (final Entry<String, Map<String, UserNodeEvents>> outerEntry : eventsByUserAndDay.entrySet())
        {
            final Map<String, UserNodeEvents> eventsByUser = outerEntry.getValue();

            // we need to have a reproducible scoring order for user events, otherwise we may end up with different scores on separate
            // scoring occasions for the same data set
            final List<String> userKeys = new ArrayList<String>(eventsByUser.keySet());
            Collections.sort(userKeys);

            for (final String userKey : userKeys)
            {
                final UserNodeEvents userEvents = eventsByUser.get(userKey);

                int checkouts = userEvents.getCheckouts();
                int checkins = userEvents.getCheckins();
                final int checkoutCancellations = userEvents.getCheckoutCancellations();

                // determine "new" checkouts not matched by either a checkin or cancellation
                checkouts -= checkoutCancellations;
                checkouts -= checkins;

                if (checkouts < 0)
                {
                    checkouts = 0;
                }

                AtomicInteger cociCount = cociCountByUser.get(userKey);
                if (cociCount == null && (checkouts > 0 || checkins > 0))
                {
                    cociCount = new AtomicInteger(0);
                    cociCountByUser.put(userKey, cociCount);

                    // subtract from inner counts as already counted
                    if (checkins > 0)
                    {
                        checkins--;
                        // first checkin, so guarantee to count it as a full one
                        cociScore += this.checkinModifier;
                    }
                    else
                    {
                        checkouts--;
                        // first checkout, so guarantee to count it as a full one
                        cociScore += this.checkoutModifier;
                    }

                    // mark as counted in penalty-related counters
                    cociCount.incrementAndGet();
                    globalCociCount.incrementAndGet();
                }

                for (int i = 0; i < checkins; i++)
                {
                    cociScore += scoreOne(globalCociCount, cociCount, this.globalCociLimitFor80Penalty, this.userCociLimitFor80Penalty,
                            this.checkinModifier);
                }

                for (int i = 0; i < checkouts; i++)
                {
                    cociScore += scoreOne(globalCociCount, cociCount, this.globalCociLimitFor80Penalty, this.userCociLimitFor80Penalty,
                            this.checkoutModifier);
                }
            }
        }

        return cociScore;
    }

    /**
     * @param globalCociLimitFor80Penalty
     *            the globalCociLimitFor80Penalty to set
     */
    public synchronized void setGlobalCociLimitFor80Penalty(double globalCociLimitFor80Penalty)
    {
        this.globalCociLimitFor80Penalty = globalCociLimitFor80Penalty;
    }

    /**
     * @param userCociLimitFor80Penalty
     *            the userCociLimitFor80Penalty to set
     */
    public synchronized void setUserCociLimitFor80Penalty(double userCociLimitFor80Penalty)
    {
        this.userCociLimitFor80Penalty = userCociLimitFor80Penalty;
    }

    /**
     * @param checkoutModifier
     *            the checkoutModifier to set
     */
    public synchronized void setCheckoutModifier(double checkoutModifier)
    {
        this.checkoutModifier = checkoutModifier;
    }

    /**
     * @param checkinModifier
     *            the checkinModifier to set
     */
    public synchronized void setCheckinModifier(double checkinModifier)
    {
        this.checkinModifier = checkinModifier;
    }

    /**
     * @param globalEditLimitFor80Penalty
     *            the globalEditLimitFor80Penalty to set
     */
    public synchronized void setGlobalEditLimitFor80Penalty(double globalEditLimitFor80Penalty)
    {
        this.globalEditLimitFor80Penalty = globalEditLimitFor80Penalty;
    }

    /**
     * @param userEditLimitFor80Penalty
     *            the userEditLimitFor80Penalty to set
     */
    public synchronized void setUserEditLimitFor80Penalty(double userEditLimitFor80Penalty)
    {
        this.userEditLimitFor80Penalty = userEditLimitFor80Penalty;
    }

    /**
     * @param editModifier
     *            the editModifier to set
     */
    public synchronized void setEditModifier(double editModifier)
    {
        this.editModifier = editModifier;
    }

    /**
     * @param globalEditContentLimitFor80Penalty
     *            the globalEditContentLimitFor80Penalty to set
     */
    public synchronized void setGlobalEditContentLimitFor80Penalty(double globalEditContentLimitFor80Penalty)
    {
        this.globalEditContentLimitFor80Penalty = globalEditContentLimitFor80Penalty;
    }

    /**
     * @param userEditContentLimitFor80Penalty
     *            the userEditContentLimitFor80Penalty to set
     */
    public synchronized void setUserEditContentLimitFor80Penalty(double userEditContentLimitFor80Penalty)
    {
        this.userEditContentLimitFor80Penalty = userEditContentLimitFor80Penalty;
    }

    /**
     * @param editContentModifier
     *            the editContentModifier to set
     */
    public synchronized void setEditContentModifier(double editContentModifier)
    {
        this.editContentModifier = editContentModifier;
    }

    /**
     * @param globalViewLimitFor80Penalty
     *            the globalViewLimitFor80Penalty to set
     */
    public synchronized void setGlobalViewLimitFor80Penalty(double globalViewLimitFor80Penalty)
    {
        this.globalViewLimitFor80Penalty = globalViewLimitFor80Penalty;
    }

    /**
     * @param userViewLimitFor80Penalty
     *            the userViewLimitFor80Penalty to set
     */
    public synchronized void setUserViewLimitFor80Penalty(double userViewLimitFor80Penalty)
    {
        this.userViewLimitFor80Penalty = userViewLimitFor80Penalty;
    }

    /**
     * @param viewModifier
     *            the viewModifier to set
     */
    public synchronized void setViewModifier(double viewModifier)
    {
        this.viewModifier = viewModifier;
    }

    /**
     * @param globalDownloadLimitFor80Penalty
     *            the globalDownloadLimitFor80Penalty to set
     */
    public synchronized void setGlobalDownloadLimitFor80Penalty(double globalDownloadLimitFor80Penalty)
    {
        this.globalDownloadLimitFor80Penalty = globalDownloadLimitFor80Penalty;
    }

    /**
     * @param userDownloadLimitFor80Penalty
     *            the userDownloadLimitFor80Penalty to set
     */
    public synchronized void setUserDownloadLimitFor80Penalty(double userDownloadLimitFor80Penalty)
    {
        this.userDownloadLimitFor80Penalty = userDownloadLimitFor80Penalty;
    }

    /**
     * @param downloadModifier
     *            the downloadModifier to set
     */
    public synchronized void setDownloadModifier(double downloadModifier)
    {
        this.downloadModifier = downloadModifier;
    }

    /**
     * @param globalRatingLimitFor80Penalty
     *            the globalRatingLimitFor80Penalty to set
     */
    public synchronized void setGlobalRatingLimitFor80Penalty(double globalRatingLimitFor80Penalty)
    {
        this.globalRatingLimitFor80Penalty = globalRatingLimitFor80Penalty;
    }

    /**
     * @param userRatingLimitFor80Penalty
     *            the userRatingLimitFor80Penalty to set
     */
    public synchronized void setUserRatingLimitFor80Penalty(double userRatingLimitFor80Penalty)
    {
        this.userRatingLimitFor80Penalty = userRatingLimitFor80Penalty;
    }

    /**
     * @param ratingModifier
     *            the ratingModifier to set
     */
    public synchronized void setRatingModifier(double ratingModifier)
    {
        this.ratingModifier = ratingModifier;
    }

    /**
     * @param globalCommentLimitFor80Penalty
     *            the globalCommentLimitFor80Penalty to set
     */
    public synchronized void setGlobalCommentLimitFor80Penalty(double globalCommentLimitFor80Penalty)
    {
        this.globalCommentLimitFor80Penalty = globalCommentLimitFor80Penalty;
    }

    /**
     * @param userCommentLimitFor80Penalty
     *            the userCommentLimitFor80Penalty to set
     */
    public synchronized void setUserCommentLimitFor80Penalty(double userCommentLimitFor80Penalty)
    {
        this.userCommentLimitFor80Penalty = userCommentLimitFor80Penalty;
    }

    /**
     * @param commentModifier
     *            the commentModifier to set
     */
    public synchronized void setCommentModifier(double commentModifier)
    {
        this.commentModifier = commentModifier;
    }

    /**
     * @param globalTagLimitFor80Penalty
     *            the globalTagLimitFor80Penalty to set
     */
    public synchronized void setGlobalTagLimitFor80Penalty(double globalTagLimitFor80Penalty)
    {
        this.globalTagLimitFor80Penalty = globalTagLimitFor80Penalty;
    }

    /**
     * @param userTagLimitFor80Penalty
     *            the userTagLimitFor80Penalty to set
     */
    public synchronized void setUserTagLimitFor80Penalty(double userTagLimitFor80Penalty)
    {
        this.userTagLimitFor80Penalty = userTagLimitFor80Penalty;
    }

    /**
     * @param tagModifier
     *            the tagModifier to set
     */
    public synchronized void setTagModifier(double tagModifier)
    {
        this.tagModifier = tagModifier;
    }

}
