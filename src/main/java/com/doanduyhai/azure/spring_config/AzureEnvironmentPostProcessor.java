/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doanduyhai.azure.spring_config;

import static com.doanduyhai.azure.spring_config.Constants.AZURE_TABLE_ENABLED_KEY;
import static com.doanduyhai.azure.spring_config.Constants.AZURE_KEYVAULT_ENABLED_KEY;
import static com.doanduyhai.azure.spring_config.azure_table.AzureTableProperties.CONNECTION_STRING_KEYVAUL_SECRET_NAME_PATTERN;
import static com.doanduyhai.azure.spring_config.utils.Validator.validateTrue;
import static java.lang.String.format;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import com.doanduyhai.azure.spring_config.azure_table.AzureTableEnvironmentProcessor;
import com.doanduyhai.azure.spring_config.azure_table.AzureTableProperties;
import com.doanduyhai.azure.spring_config.keyvault.KeyVaultEnvironmentProcessor;
import com.doanduyhai.azure.spring_config.keyvault.KeyVaultProperties;
import com.doanduyhai.azure.spring_config.keyvault.KeyVaultProperties.Property;

@Component
/**
 * Leverage {@link EnvironmentPostProcessor} to add Key Vault secrets as a property source.
 */
public class AzureEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered, ApplicationListener<ApplicationEvent> {

    public static final int DEFAULT_ORDER = ConfigFileApplicationListener.DEFAULT_ORDER + 1;
    private int order = DEFAULT_ORDER;

    private static final DeferredLog logger = new DeferredLog();

    private ConfigurableEnvironment environment;

    /**
     * Post process the environment.
     *
     * <p>
     * Here we are going to process any key vault(s) and make them as available
     * PropertySource(s). Note this supports both the singular key vault setup,
     * as well as the multiple key vault setup.
     * </p>
     *
     * @param environment the environment.
     * @param application the application.
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        this.environment = environment;
        final KeyVaultEnvironmentProcessor helper = new KeyVaultEnvironmentProcessor(logger, environment);
        if (isKeyVaultEnabled(environment)) {
            helper.addKeyVaultPropertySource();
            maybeConfigureAzureTable(environment, helper);
        }

        if (this.environment != null) {
            logger.info("Spring property sources: ");
            environment.getPropertySources()
                    .stream()
                    .forEach(propertySource -> logger.info(format("\t %s", propertySource.getName())));
        }
    }

    /**
     * Is the key vault enabled.
     *
     * @param environment    the environment.
     * @return true if the key vault is enabled, false otherwise.
     */
    private boolean isKeyVaultEnabled(ConfigurableEnvironment environment) {
        return environment.getProperty(
                KeyVaultProperties.getPropertyName(Property.ENABLED),
                Boolean.class,
                true)
            && environment.getProperty(KeyVaultProperties.getPropertyName(Property.URI)) != null
            && isKeyVaultClientAvailable();
    }

    private void maybeConfigureAzureTable(ConfigurableEnvironment environment, KeyVaultEnvironmentProcessor keyVaultHelper) {
        Boolean tableEnabled = environment.getProperty(AzureTableProperties.getPropertyName(AzureTableProperties.Property.ENABLED),
                Boolean.class,
                true);

        if (tableEnabled) {
            validateTrue(isKeyVaultEnabled(environment), format("If '%s' = true then '%s' should be true and the key vault configured properly", AZURE_TABLE_ENABLED_KEY, AZURE_KEYVAULT_ENABLED_KEY));
            String storageAccountNameProperty = AzureTableProperties.getPropertyName(AzureTableProperties.Property.STORAGE_ACCOUNT_NAME);
            String storageAccountName = environment.getProperty(storageAccountNameProperty);
            validateTrue(StringUtils.isNotBlank(storageAccountName), format("If '%s' = true, then you should provide the property '%s'", AZURE_TABLE_ENABLED_KEY, storageAccountNameProperty));

            String tableNameProperty = AzureTableProperties.getPropertyName(AzureTableProperties.Property.TABLE_NAME);
            String tableName = environment.getProperty(tableNameProperty);
            validateTrue(StringUtils.isNotBlank(tableName), format("If '%s' = true, then you should provide the property '%s'", AZURE_TABLE_ENABLED_KEY, tableNameProperty));

            String keyVaultSecretName = format(CONNECTION_STRING_KEYVAUL_SECRET_NAME_PATTERN, storageAccountName);
            String tableConnectionString = keyVaultHelper.getKeyVaultSecret(keyVaultSecretName);
            validateTrue(StringUtils.isNotBlank(tableConnectionString), format("The '%s' secret should be present in the key vault '%s'", keyVaultSecretName, keyVaultHelper.getVaultUri()));

            AzureTableEnvironmentProcessor azureTableEnvironmentProcessor = new AzureTableEnvironmentProcessor(logger, environment);
            azureTableEnvironmentProcessor.addAzureTablePropertySource(tableConnectionString, tableName);
        }

    }


    private boolean isKeyVaultClientAvailable() {
        return ClassUtils.isPresent("com.azure.security.keyvault.secrets.SecretClient",
            AzureEnvironmentPostProcessor.class.getClassLoader());
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        logger.replayTo(AzureEnvironmentPostProcessor.class);

    }
}
