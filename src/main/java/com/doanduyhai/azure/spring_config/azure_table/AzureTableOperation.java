package com.doanduyhai.azure.spring_config.azure_table;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.microsoft.azure.storage.ResultContinuation;
import com.microsoft.azure.storage.core.SR;
import com.microsoft.azure.storage.table.DynamicTableEntity;
import com.microsoft.azure.storage.table.EntityProperty;

public class AzureTableOperation {

    private static final String PROPERTY_VALUE_ROW_KEY = "propertyValue";
    private static final String COLLECTION_INDEX_PREFIX = "_";
    private final PropertyValueDao propertyValueDao;

    public AzureTableOperation(String connectionString, String tableName) {
        this.propertyValueDao = new PropertyValueDao(connectionString, tableName);
    }


    public String[] getPropertyNames() {
        List<String> propertyNames = new ArrayList<>();

        AzureTableDao<DynamicTableEntity>.FullScanQuery fullScanQuery = propertyValueDao.fullScanQuery();

        Pair<List<DynamicTableEntity>, ResultContinuation> listResultContinuationPair = fullScanQuery
                .pageSize(100)
                .listFirstPage();

        listResultContinuationPair
                .getLeft()
                .stream()
                .map(DynamicTableEntity::getPartitionKey)
                .forEach(propertyNames::add);

        ResultContinuation continuationToken = listResultContinuationPair.getRight();

        while (continuationToken != null) {
            Pair<List<DynamicTableEntity>, ResultContinuation> result = fullScanQuery.listNextPage(continuationToken);

            continuationToken = result.getRight();
            result
                    .getLeft()
                    .stream()
                    .map(DynamicTableEntity::getPartitionKey)
                    .forEach(propertyNames::add);
        }

        return propertyNames.toArray(new String[propertyNames.size()]);
    }


    public Object getProperty(String propertyName) {
        String normalizePropertyName = normalizePropertyName(propertyName);
        DynamicTableEntity foundEntity = propertyValueDao.crud().findById(normalizePropertyName, PROPERTY_VALUE_ROW_KEY);
        if (foundEntity != null) {
            HashMap<String, EntityProperty> properties = foundEntity.getProperties();
            if (properties.size() == 1) {
                return properties
                        .values()
                        .stream()
                        .limit(1L)
                        .filter(entityProperty -> !entityProperty.getIsNull())
                        .findFirst()
                        .map(AzureTableOperation::mapValueByType)
                        .orElse(null);
            } else {
                boolean isKeyIndexValue = properties
                        .keySet()
                        .stream()
                        .allMatch(checkKeyIsInteger());

                if (isKeyIndexValue) {
                    return properties
                            .entrySet()
                            .stream()
                            .sorted((e1, e2) -> {
                                Integer index1 = Integer.parseInt(e1.getKey().replaceFirst(COLLECTION_INDEX_PREFIX, ""));
                                Integer index2 = Integer.parseInt(e2.getKey().replaceFirst(COLLECTION_INDEX_PREFIX, ""));
                                return index1.compareTo(index2);
                            })
                            .map(entry -> mapValueByType(entry.getValue()))
                            .collect(toList());
                } else {
                    return properties
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> mapValueByType(entry.getValue())));
                }
            }
        } else {
            return null;
        }
    }

    private Predicate<String> checkKeyIsInteger() {
        return key -> {
            if (key.startsWith("_")) {
                String formattedKey = key.replaceFirst(COLLECTION_INDEX_PREFIX, "");
                try {
                    int index = Integer.parseInt(formattedKey);
                    return index >= 0;
                } catch (NumberFormatException ex) {
                    return false;
                }
            } else {
                return false;
            }
        };
    }

    /**
     * https://docs.microsoft.com/en-us/rest/api/storageservices/understanding-the-table-service-data-model#characters-disallowed-in-key-fields
     */
    private static String normalizePropertyName(String propertyName) {
        return propertyName.replaceAll("(?:/|#|\\?|\t|\n|\r|\\\\)", "");
    }
    private static Object mapValueByType(EntityProperty entityProperty) {
        Class<?> type = entityProperty.getType();
        if (type.equals(byte[].class)) {
            return entityProperty.getValueAsByteArray();
        }
        else if (type.equals(Byte[].class)) {
            return entityProperty.getValueAsByteObjectArray();
        }
        else if (type.equals(String.class)) {
            return entityProperty.getValueAsString();
        }
        else if (type.equals(boolean.class)) {
            return entityProperty.getValueAsBoolean();
        }
        else if (type.equals(Boolean.class)) {
            return entityProperty.getValueAsBooleanObject();
        }
        else if (type.equals(Date.class)) {
            return entityProperty.getValueAsDate();
        }
        else if (type.equals(double.class)) {
            return entityProperty.getValueAsDouble();
        }
        else if (type.equals(Double.class)) {
            return entityProperty.getValueAsDoubleObject();
        }
        else if (type.equals(UUID.class)) {
            return entityProperty.getValueAsUUID();
        }
        else if (type.equals(int.class)) {
            return entityProperty.getValueAsInteger();
        }
        else if (type.equals(Integer.class)) {
            return entityProperty.getValueAsIntegerObject();
        }
        else if (type.equals(long.class)) {
            return entityProperty.getValueAsLong();
        }
        else if (type.equals(Long.class)) {
            return entityProperty.getValueAsLongObject();
        }
        else {
            throw new IllegalArgumentException(String.format(SR.TYPE_NOT_SUPPORTED, type.toString()));
        }
    }
}
