// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.doanduyhai.azure.spring_config.keyvault;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.doanduyhai.azure.spring_config.Constants;


@ConfigurationProperties(value = KeyVaultProperties.PREFIX)
public class KeyVaultProperties {

    public static final String PREFIX = "azure.keyvault";
    public static final String DELIMITER = ".";


    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Long getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Long refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public List<String> getSecretKeys() {
        return secretKeys;
    }

    public void setSecretKeys(List<String> secretKeys) {
        this.secretKeys = secretKeys;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }


    private Boolean enabled;
    private List<String> secretKeys;
    private Long refreshInterval = Constants.DEFAULT_REFRESH_INTERVAL_MS;

    /**
     * The constant used to define the order of the key vaults you are
     * delivering (comma delimited, e.g 'my-vault, my-vault-2').
     */
    private String order;
    private String uri;


    public enum Property {
        CASE_SENSITIVE_KEYS("case-sensitive-keys"),
        ENABLED("enabled"),
        ORDER("order"),
        REFRESH_INTERVAL("refresh-interval"),
        SECRET_KEYS("secret-keys"),
        URI("uri");

        private final String name;

        String getName() {
            return name;
        }

        Property(String name) {
            this.name = name;
        }
    }

    public static String getPropertyName(Property property) {
        return String.join(DELIMITER, PREFIX, property.getName());
    }


}
