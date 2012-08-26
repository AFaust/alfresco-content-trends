package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import java.util.Collection;

import org.alfresco.service.cmr.repository.NodeRef;

import com.prodyna.alfresco.contenttrends.repo.module.audit.ContentTrendsEventType;

public class UserNodeEvents
{
    public static enum ScorableNodeEventType
    {
        DOWNLOADS, VIEWS,

        COPIES,

        CONTENT_EDITS, EDITS,

        CHECKINS, CHECKOUTS, CHECKOUT_CANCELLATIONS,

        RATINGS, COMMENTS, TAGS;
    }

    private int copies = 0;
    private int edits = 0;
    private int contentEdits = 0;

    private int checkouts = 0;
    private int checkins = 0;
    private int checkoutCancellations = 0;

    private int ratings = 0;
    private int comments = 0;
    private int tags = 0;

    private int views = 0;
    private int downloads = 0;

    /**
     * @return the copies
     */
    public int getCopies()
    {
        return copies;
    }

    /**
     * @return the edits
     */
    public int getEdits()
    {
        return edits;
    }

    /**
     * @return the contentEdits
     */
    public int getContentEdits()
    {
        return contentEdits;
    }

    /**
     * @return the checkouts
     */
    public int getCheckouts()
    {
        return checkouts;
    }

    /**
     * @return the checkins
     */
    public int getCheckins()
    {
        return checkins;
    }

    /**
     * @return the checkoutCancellations
     */
    public int getCheckoutCancellations()
    {
        return checkoutCancellations;
    }

    /**
     * @return the ratings
     */
    public int getRatings()
    {
        return ratings;
    }

    /**
     * @return the comments
     */
    public int getComments()
    {
        return comments;
    }

    /**
     * @return the tags
     */
    public int getTags()
    {
        return tags;
    }

    /**
     * @return the views
     */
    public int getViews()
    {
        return views;
    }

    /**
     * @return the downloads
     */
    public int getDownloads()
    {
        return downloads;
    }

    public int getCount(final ScorableNodeEventType eventType)
    {
        final int result;

        switch (eventType)
        {
        case VIEWS:
            result = this.views;
            break;
        case DOWNLOADS:
            result = this.downloads;
            break;
        case EDITS:
            result = this.edits;
            break;
        case CONTENT_EDITS:
            result = this.contentEdits;
            break;
        case TAGS:
            result = this.tags;
            break;
        case RATINGS:
            result = this.ratings;
            break;
        case COMMENTS:
            result = this.comments;
            break;
        case COPIES:
            result = this.copies;
            break;
        case CHECKOUTS:
            result = this.checkouts;
            break;
        case CHECKOUT_CANCELLATIONS:
            result = this.checkoutCancellations;
            break;
        case CHECKINS:
            result = this.checkins;
            break;
        default:
            result = 0;
        }

        return result;
    }

    public void addEvents(final ContentTrendsEventType eventType, final int amount, final Collection<NodeRef> relatedNodes)
    {
        if (amount > 0)
        {
            switch (eventType)
            {
            case COPY:
                this.copies += amount;
                break;
            case EDIT:
                this.edits += amount;
                break;
            case EDIT_CONTENT:
                this.contentEdits += amount;
                break;
            case TAG:
                this.tags += amount;
                break;
            case RATE:
                this.ratings += amount;
                break;
            case COMMENT:
                this.comments += amount;
                break;
            case COCI:
                final int checkins = (relatedNodes != null ? relatedNodes.size() : 0);
                final int checkouts = amount - checkins;
                this.checkins += checkins;
                this.checkouts += checkouts > 0 ? checkouts : 0;
                break;
            case CANCEL_COCI:
                this.checkoutCancellations += amount;
                break;
            case LOCK:
                // don't actually care at the moment
                break;
            case VIEW:
                views += amount;
                break;
            case DOWNLOAD:
                downloads += amount;
                break;
            // default:
            // not necessary at the moment
            }
        }
    }
}