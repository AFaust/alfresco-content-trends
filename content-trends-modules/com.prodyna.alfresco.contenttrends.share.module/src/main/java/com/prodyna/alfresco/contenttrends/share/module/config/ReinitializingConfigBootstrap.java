/**
 * 
 */
package com.prodyna.alfresco.contenttrends.share.module.config;

import org.springframework.extensions.config.ConfigBootstrap;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ReinitializingConfigBootstrap extends ConfigBootstrap
{

    /**
     * {@inheritDoc}
     */
    @Override
    public void register()
    {
        super.register();

        this.configService.reset();
    }
}
