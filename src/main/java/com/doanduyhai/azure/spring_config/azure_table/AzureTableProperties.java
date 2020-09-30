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
