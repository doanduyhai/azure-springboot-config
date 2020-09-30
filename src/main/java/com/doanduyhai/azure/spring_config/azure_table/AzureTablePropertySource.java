// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.doanduyhai.azure.spring_config.azure_table;

import static com.doanduyhai.azure.spring_config.Constants.AZURE_TABLE_PROPERTYSOURCE_NAME;

import org.springframework.core.env.EnumerablePropertySource;


/**
 * An Azure table implementation of {@link EnumerablePropertySource} to enumerate all property pairs in Key Vault.
 */
public class AzureTablePropertySource extends EnumerablePropertySource<AzureTableOperation> {

    private final AzureTableOperation operations;

    public AzureTablePropertySource(String keyVaultName, AzureTableOperation operation) {
        super(keyVaultName, operation);
        this.operations = operation;
    }

    public AzureTablePropertySource(AzureTableOperation operation) {
        super(AZURE_TABLE_PROPERTYSOURCE_NAME, operation);
        this.operations = operation;
    }

    @Override
    public String[] getPropertyNames() {
        return this.operations.getPropertyNames();
    }

    @Override
    public Object getProperty(String name) {
        return operations.getProperty(name);
    }
}
