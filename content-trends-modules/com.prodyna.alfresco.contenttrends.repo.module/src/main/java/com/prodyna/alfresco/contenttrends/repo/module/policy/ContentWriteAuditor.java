/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.policy;

import java.io.Serializable;
import java.util.Collections;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.audit.AuditComponent;
import org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * This policy is used to audit successful content writes for the ContentTrendsBase audit application. It is not possible to audit this kind
 * of event via Alfresco services as the write is actually deferred to the {@link ContentWriter} returned by
 * {@link ContentService#getWriter(NodeRef, org.alfresco.service.namespace.QName, boolean) getWriter}. This leaves audit applications unable
 * to determine if the write was successful based on the {@code /alfresco-api/post/ContentService/getWriter/no-error} value.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ContentWriteAuditor implements OnContentUpdatePolicy, InitializingBean
{

    private static final String ROOT_PATH = "/ContentTrendsBase/ContentAuditor/writeContent";

    private PolicyComponent policyComponent;

    private AuditComponent auditComponent;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onContentUpdate(NodeRef nodeRef, boolean newContent)
    {
        this.auditComponent.recordAuditValues(ROOT_PATH, Collections.<String, Serializable> singletonMap("nodeRef", nodeRef));
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "policyComponent", this.policyComponent);
        PropertyCheck.mandatory(this, "auditComponent", this.auditComponent);

        this.policyComponent.bindClassBehaviour(OnContentUpdatePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this,
                "onContentUpdate", NotificationFrequency.EVERY_EVENT));
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
