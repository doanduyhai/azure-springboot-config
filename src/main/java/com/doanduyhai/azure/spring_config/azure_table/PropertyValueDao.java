package com.doanduyhai.azure.spring_config.azure_table;

import com.microsoft.azure.storage.table.DynamicTableEntity;

public class PropertyValueDao extends AzureTableDao<DynamicTableEntity> {


    public PropertyValueDao(String storageConnectionString, String tableName) {
        super(DynamicTableEntity.class, storageConnectionString, tableName);
    }

}
