/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.policy;

import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.CopyServicePolicies.OnCopyNodePolicy;
import org.alfresco.repo.copy.DoNothingCopyBehaviourCallback;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

import com.prodyna.alfresco.contenttrends.repo.module.model.ContentTrendsModel;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScoredAspect implements OnCopyNodePolicy, InitializingBean
{
    private PolicyComponent policyComponent;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "policyComponent", this.policyComponent);

        this.policyComponent.bindClassBehaviour(OnCopyNodePolicy.QNAME, ContentTrendsModel.ASPECT_SCORED, new JavaBehaviour(this,
                "getCopyCallback", NotificationFrequency.EVERY_EVENT));
        this.policyComponent.bindClassBehaviour(OnCopyNodePolicy.QNAME, ContentTrendsModel.ASPECT_SCORED_CHANGE, new JavaBehaviour(this,
                "getCopyCallback", NotificationFrequency.EVERY_EVENT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails)
    {
        return DoNothingCopyBehaviourCallback.getInstance();
    }

    /**
     * @param policyComponent
     *            the policyComponent to set
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

}
