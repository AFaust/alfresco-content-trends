/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.jobs;

import org.alfresco.repo.audit.AuditComponent;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.JobLockService.JobLockRefreshCallback;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VmShutdownListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractContentTrendsProcessor implements InitializingBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContentTrendsProcessor.class);
    private static final VmShutdownListener VM_SHUTDOWN_LISTENER = new VmShutdownListener(AbstractContentTrendsProcessor.class.getName());

    private TransactionService transactionService;
    private JobLockService jobLockService;

    private volatile boolean busy;

    protected AuditService auditService;
    protected AuditComponent auditComponent;

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "auditComponent", this.auditComponent);
        PropertyCheck.mandatory(this, "auditService", this.auditService);

        PropertyCheck.mandatory(this, "transactionService", this.transactionService);
        PropertyCheck.mandatory(this, "jobLockService", this.jobLockService);
    }

    /**
     * @param auditService
     *            the auditService to set
     */
    public void setAuditService(AuditService auditService)
    {
        this.auditService = auditService;
    }

    /**
     * @param auditComponent
     *            the auditComponent to set
     */
    public void setAuditComponent(AuditComponent auditComponent)
    {
        this.auditComponent = auditComponent;
    }

    /**
     * @param transactionService
     *            the transactionService to set
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * @param jobLockService
     *            the jobLockService to set
     */
    public void setJobLockService(JobLockService jobLockService)
    {
        this.jobLockService = jobLockService;
    }

    protected final void processImpl()
    {
        // Note: the framework of this method is based on AbstractFeedGenerator

        // Avoid running when in read-only mode
        if (!this.transactionService.getAllowWrite())
        {
            LOGGER.trace("{} not running due to read-only server", getClass().getSimpleName());
            return;
        }

        String lockToken = null;

        try
        {
            final JobLockRefreshCallback lockCallback = new LockCallback();
            lockToken = acquireLock(lockCallback);

            this.transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>()
            {

                @Override
                public Void execute() throws Throwable
                {
                    // this is a system process
                    AuthenticationUtil.runAsSystem(new RunAsWork<Void>()
                    {

                        @Override
                        public Void doWork() throws Exception
                        {
                            doProcess();
                            return null;
                        }
                    });
                    return null;
                }
            });
        }
        catch (LockAcquisitionException e)
        {
            // Job being done by another process
            LOGGER.debug("{} already running", getClass().getSimpleName());
        }
        catch (Throwable e)
        {
            // If the VM is shutting down, then ignore
            if (VM_SHUTDOWN_LISTENER.isVmShuttingDown())
            {
                // Ignore
            }
            else
            {
                LOGGER.error("Exception during prcessing run", e);
            }
        }
        finally
        {
            releaseLock(lockToken);
        }
    }

    abstract protected void doProcess();

    abstract protected QName getLockQName();

    abstract protected long getLockTTL();

    private final String acquireLock(JobLockRefreshCallback lockCallback) throws LockAcquisitionException
    {
        // Try to get lock
        final String lockToken = this.jobLockService.getLock(getLockQName(), getLockTTL());

        // Got the lock - now register the refresh callback which will keep the lock alive
        this.jobLockService.refreshLock(lockToken, getLockQName(), getLockTTL(), lockCallback);

        this.busy = true;

        LOGGER.debug("lock aquired: {}", lockToken);

        return lockToken;
    }

    private final void releaseLock(String lockToken)
    {
        if (lockToken != null)
        {
            this.busy = false;
            this.jobLockService.releaseLock(lockToken, getLockQName());
            LOGGER.debug("lock released: {}", lockToken);
        }
    }

    private class LockCallback implements JobLockRefreshCallback
    {
        @Override
        public boolean isActive()
        {
            return AbstractContentTrendsProcessor.this.busy;
        }

        @Override
        public void lockReleased()
        {
            // note: currently the cycle will try to complete (even if refresh failed)
            synchronized (this)
            {
                LOGGER.debug("Lock released (refresh failed): {}", AbstractContentTrendsProcessor.this.getLockQName());
                AbstractContentTrendsProcessor.this.busy = false;
            }
        }
    }
}
