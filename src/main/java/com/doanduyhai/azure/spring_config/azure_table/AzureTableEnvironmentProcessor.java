package com.doanduyhai.azure.spring_config.azure_table;

import static com.doanduyhai.azure.spring_config.Constants.AZURE_KEYVAULT_PROPERTYSOURCE_NAME;
import static com.doanduyhai.azure.spring_config.Constants.AZURE_TABLE_PROPERTYSOURCE_NAME;
import static java.lang.String.format;

import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

public class AzureTableEnvironmentProcessor {

    private final DeferredLog logger;

    private final ConfigurableEnvironment environment;

    public AzureTableEnvironmentProcessor(DeferredLog logger, ConfigurableEnvironment environment) {
        this.logger = logger;
        this.environment = environment;
    }

    public void addAzureTablePropertySource(String connectionString, String tableName) {
        logger.info(format( "Adding Azure table '%s' as a Spring property source", tableName));
        final MutablePropertySources sources = this.environment.getPropertySources();
        final AzureTableOperation azureTableOperation = new AzureTableOperation(connectionString, tableName);

        AzureTablePropertySource propertySource = new AzureTablePropertySource(AZURE_TABLE_PROPERTYSOURCE_NAME, azureTableOperation);
        sources.addAfter(AZURE_KEYVAULT_PROPERTYSOURCE_NAME, propertySource);

    }
}
