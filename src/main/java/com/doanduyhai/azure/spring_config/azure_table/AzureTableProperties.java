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
package com.doanduyhai.azure.spring_config.azure_table;

import static com.doanduyhai.azure.spring_config.azure_table.AzureTableProperties.PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = PREFIX)
public class AzureTableProperties {
    public static final String PREFIX = "azure.table";
    public static final String CONNECTION_STRING_KEYVAUL_SECRET_NAME_PATTERN = "%s-connection-string";

    private Boolean enabled;
    private Boolean name;

    public enum Property {
        STORAGE_ACCOUNT_NAME("storage-account-name"),
        TABLE_NAME("table-name"),
        ENABLED("enabled");

        private final String name;

        String getName() {
            return name;
        }

        Property(String name) {
            this.name = name;
        }
    }

    public static String getPropertyName(Property property) {
        return String.format("%s.%s", PREFIX, property.getName());
    }
}
