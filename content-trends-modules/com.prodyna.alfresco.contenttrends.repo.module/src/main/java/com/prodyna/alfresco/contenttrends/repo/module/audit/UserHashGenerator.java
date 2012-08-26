/**
 * 
 */
package com.prodyna.alfresco.contenttrends.repo.module.audit;

import java.io.Serializable;

import org.alfresco.repo.audit.generator.AbstractDataGenerator;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PersonService;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class UserHashGenerator extends AbstractDataGenerator
{

    private PersonService personService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable getData() throws Throwable
    {
        final String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();

        final Serializable data;
        if (this.personService.personExists(fullyAuthenticatedUser))
        {
            final NodeRef personRef = this.personService.getPerson(fullyAuthenticatedUser);
            // TODO: retrieve (or lazy create) hash from person
            data = personRef.getId();
        }
        else if(AuthenticationUtil.isRunAsUserTheSystemUser())
        {
            data = AuthenticationUtil.getSystemUserName();
        } else {
            data = null;
        }

        return data;
    }

    /**
     * @param personService
     *            the personService to set
     */
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

}
