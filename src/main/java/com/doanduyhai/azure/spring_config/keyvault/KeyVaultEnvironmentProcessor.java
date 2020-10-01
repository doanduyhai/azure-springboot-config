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
package com.doanduyhai.azure.spring_config.keyvault;

import static com.doanduyhai.azure.spring_config.Constants.*;
import static java.lang.String.format;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.doanduyhai.azure.spring_config.keyvault.KeyVaultProperties.Property;
import com.doanduyhai.azure.spring_config.utils.Validator;

/**
 * A helper class to initialize the key vault secret client using MSI authentication. Then
 * add key vault as a property source to the environment.
 */
public class KeyVaultEnvironmentProcessor {

    private static final TokenCredential MSI_TOKEN_CREDENTIALS = new ManagedIdentityCredentialBuilder().build();

    private final ConfigurableEnvironment environment;
    private final String vaultUri;
    private final SecretClient secretClient;
    private final DeferredLog logger;

    public KeyVaultEnvironmentProcessor(DeferredLog logger, final ConfigurableEnvironment environment) {
        this.environment = environment;
        Validator.validateNotNull(environment, "Spring configurable environment");
        vaultUri = getPropertyValue(Property.URI);
        Validator.validateNotBlank(vaultUri, AZURE_KEYVAULT_URI);
        secretClient = new SecretClientBuilder()
                .vaultUrl(vaultUri)
                .credential(MSI_TOKEN_CREDENTIALS)
                .httpLogOptions(new HttpLogOptions().setApplicationId(AZURE_SPRING_KEY_VAULT))
                .buildClient();
        this.logger = logger;
    }

    public void addKeyVaultPropertySource() {
        logger.info(format("Adding Azure key vault '%s' as a Spring property source", vaultUri));
        final Long refreshInterval = Optional.ofNullable(getPropertyValue(Property.REFRESH_INTERVAL))
                .map(Long::valueOf)
                .orElse(DEFAULT_REFRESH_INTERVAL_MS);
        final List<String> secretKeys = Binder.get(this.environment)
                .bind(
                        KeyVaultProperties.getPropertyName( Property.SECRET_KEYS),
                        Bindable.listOf(String.class)
                )
                .orElse(Collections.emptyList());

        try {
            final MutablePropertySources sources = this.environment.getPropertySources();
            final boolean caseSensitive = Boolean
                    .parseBoolean(getPropertyValue(Property.CASE_SENSITIVE_KEYS));
            final KeyVaultOperation keyVaultOperation = new KeyVaultOperation(
                    secretClient,
                    refreshInterval,
                    secretKeys,
                    caseSensitive);

            KeyVaultPropertySource keyVaultPropertySource =
                    new KeyVaultPropertySource(AZURE_KEYVAULT_PROPERTYSOURCE_NAME, keyVaultOperation);
            if (sources.contains(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
                sources.addAfter(
                        SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        keyVaultPropertySource
                );
            } else {
                sources.addFirst(keyVaultPropertySource);
            }

        } catch (final Exception ex) {
            throw new IllegalStateException("Failed to configure KeyVault property source", ex);
        }
    }


    public String getKeyVaultSecret(String secretName) {
        logger.info(format("Retrieving secret '%s' from Azure key vault '%s' ", secretName, vaultUri));
        KeyVaultSecret secret = secretClient.getSecret(secretName);
        return secret.getValue();
    }

    public String getVaultUri() {
        return vaultUri;
    }

    private String getPropertyValue(final Property property) {
        return Optional.of(property)
                .map(KeyVaultProperties::getPropertyName)
                .map(environment::getProperty)
                .orElse(null);
    }
}
