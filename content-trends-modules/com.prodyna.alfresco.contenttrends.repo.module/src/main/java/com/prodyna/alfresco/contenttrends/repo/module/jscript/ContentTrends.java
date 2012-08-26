/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.jscript;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.ParameterCheck;

import com.prodyna.alfresco.contenttrends.repo.module.service.ContentTrendsService;
import com.prodyna.alfresco.contenttrends.repo.module.service.DatedNodeScores;
import com.prodyna.alfresco.contenttrends.repo.module.service.NodeScoreType;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ContentTrends extends BaseScopableProcessorExtension implements InitializingBean
{

    protected ContentTrendsService contentTrendsService;

    public void setContentTrendsService(final ContentTrendsService contentTrendsService)
    {
        this.contentTrendsService = contentTrendsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "contentTrendsService", this.contentTrendsService);
    }

    public Scriptable getScoreHistory(final ScriptNode node, final int numberOfDaysBack)
    {
        ParameterCheck.mandatory("node", node);
        final List<DatedNodeScores> scoreHistory = this.contentTrendsService.getScoreHistory(node.getNodeRef(), numberOfDaysBack);
        final Scriptable result = toJavaScript(scoreHistory);
        return result;
    }

    public Scriptable getScoreHistory(final ScriptNode node)
    {
        ParameterCheck.mandatory("node", node);

        final List<DatedNodeScores> scoreHistory = this.contentTrendsService.getScoreHistory(node.getNodeRef());
        final Scriptable result = toJavaScript(scoreHistory);
        return result;
    }

    public Scriptable getScoreKeys()
    {
        final NodeScoreType[] scoreTypes = NodeScoreType.values();
        final Object[] elements = new Object[scoreTypes.length];
        for (int i = 0; i < elements.length; i++)
        {
            elements[i] = scoreTypes[i].getScoreProperty().getLocalName();
        }

        final Scriptable result = Context.getCurrentContext().newArray(getScope(), elements);
        return result;
    }

    protected Scriptable toJavaScript(final List<DatedNodeScores> scoreHistory)
    {
        final Object[] entries = new Object[scoreHistory.size()];

        for (int i = 0, max = scoreHistory.size(); i < max; i++)
        {
            final DatedNodeScores scores = scoreHistory.get(i);

            final Map<String, Object> data = new HashMap<String, Object>();
            data.put("date", scores.getDate());

            for (final NodeScoreType scoreType : NodeScoreType.values())
            {
                data.put(scoreType.getScoreProperty().getLocalName(), scores.getScore(scoreType));
            }

            entries[i] = data;
        }

        final Scriptable result = Context.getCurrentContext().newArray(getScope(), entries);
        return result;
    }
}
