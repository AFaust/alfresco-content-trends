/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;

import org.alfresco.repo.audit.generator.AbstractDataGenerator;
import org.alfresco.repo.audit.generator.TransactionIdDataGenerator;
import org.alfresco.repo.domain.node.TransactionEntity;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;

/**
 * An alternative to {@link TransactionIdDataGenerator} which generates the actual DB ID of the current transaction, not a String-based
 * token. The advantage of using the actual ID lies in the ability to query the transaction during the audit information evaluation, e.g. to
 * determine if the transaction actually succeeded or failed, in which case events for ContentTrends need to be disregarded.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class TransactionDbIdGenerator extends AbstractDataGenerator
{

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable getData() throws Throwable
    {
        // constant from AbstractNodeDAOImpl.KEY_TRANSACTION
        final TransactionEntity transaction = AlfrescoTransactionSupport.getResource("node.transaction.id");
        // Note: This does not work in some cases, e.g. module start
        return transaction.getId();
    }

}
