/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.policy;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.audit.AuditComponent;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.rating.RatingService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * This policy is used to audit rating changes for the ContentTrendsBase audit application. It is not possible to audit this event via
 * Alfresco services as the {@link RatingService} itself is not auditable and it uses the private {@link NodeService} bean internally, which
 * lacks the necessary {@link AuditMethodInteceptor} to record any values.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RatingAuditor implements InitializingBean, OnCreateNodePolicy, BeforeDeleteNodePolicy, OnUpdatePropertiesPolicy
{
    private static final String KEY_NODE_REF = "nodeRef";

    private static final String ROOT_PATH = "/ContentTrendsBase/RatingAuditor/";
    private static final String CREATE_RATING_ROOT_PATH = ROOT_PATH + "createRating";
    private static final String UPDATE_RATING_ROOT_PATH = ROOT_PATH + "updateRating";
    private static final String DELETE_RATING_ROOT_PATH = ROOT_PATH + "deleteRating";

    private PolicyComponent policyComponent;

    private AuditComponent auditComponent;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        final Map<String, Serializable> auditData = new HashMap<String, Serializable>();
        auditData.put(KEY_NODE_REF, nodeRef);
        auditData.put("properties", (Serializable) new HashMap<QName, Serializable>(after));

        this.auditComponent.recordAuditValues(UPDATE_RATING_ROOT_PATH, auditData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeDeleteNode(NodeRef nodeRef)
    {
        this.auditComponent.recordAuditValues(DELETE_RATING_ROOT_PATH,
                Collections.<String, Serializable> singletonMap(KEY_NODE_REF, nodeRef));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        this.auditComponent.recordAuditValues(CREATE_RATING_ROOT_PATH,
                Collections.<String, Serializable> singletonMap(KEY_NODE_REF, childAssocRef.getChildRef()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "policyComponent", this.policyComponent);
        PropertyCheck.mandatory(this, "auditComponent", this.auditComponent);

        this.policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_RATING, new JavaBehaviour(this, "onCreateNode",
                NotificationFrequency.EVERY_EVENT));
        this.policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, ContentModel.TYPE_RATING, new JavaBehaviour(this,
                "beforeDeleteNode", NotificationFrequency.EVERY_EVENT));
        this.policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, ContentModel.TYPE_RATING, new JavaBehaviour(this,
                "onUpdateProperties", NotificationFrequency.EVERY_EVENT));
    }

    /**
     * @param policyComponent
     *            the policyComponent to set
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    /**
     * @param auditComponent
     *            the auditComponent to set
     */
    public void setAuditComponent(AuditComponent auditComponent)
    {
        this.auditComponent = auditComponent;
    }

}
