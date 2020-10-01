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

import static com.doanduyhai.azure.spring_config.Constants.AZURE_KEYVAULT_PROPERTYSOURCE_NAME;

import org.springframework.core.env.EnumerablePropertySource;

/**
 * A key vault implementation of {@link EnumerablePropertySource} to enumerate all property pairs in Key Vault.
 */
public class KeyVaultPropertySource extends EnumerablePropertySource<KeyVaultOperation> {

    private final KeyVaultOperation operations;

    public KeyVaultPropertySource(String keyVaultName, KeyVaultOperation operation) {
        super(keyVaultName, operation);
        this.operations = operation;
    }

    public KeyVaultPropertySource(KeyVaultOperation operation) {
        super(AZURE_KEYVAULT_PROPERTYSOURCE_NAME, operation);
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
