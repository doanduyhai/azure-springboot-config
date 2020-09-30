package com.doanduyhai.azure.spring_config.azure_table;

import static com.microsoft.azure.storage.table.TableQuery.QueryComparisons.EQUAL;
import static com.microsoft.azure.storage.table.TableQuery.QueryComparisons.GREATER_THAN;
import static com.microsoft.azure.storage.table.TableQuery.QueryComparisons.GREATER_THAN_OR_EQUAL;
import static com.microsoft.azure.storage.table.TableQuery.QueryComparisons.LESS_THAN;
import static com.microsoft.azure.storage.table.TableQuery.QueryComparisons.LESS_THAN_OR_EQUAL;
import static com.microsoft.azure.storage.table.TableQuery.combineFilters;
import static com.microsoft.azure.storage.table.TableQuery.generateFilterCondition;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.capitalize;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.doanduyhai.azure.spring_config.utils.Validator;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.LocationMode;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.ResultContinuation;
import com.microsoft.azure.storage.ResultSegment;
import com.microsoft.azure.storage.RetryExponentialRetry;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.EdmType;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TablePayloadFormat;
import com.microsoft.azure.storage.table.TableQuery;
import com.microsoft.azure.storage.table.TableQuery.Operators;
import com.microsoft.azure.storage.table.TableRequestOptions;
import com.microsoft.azure.storage.table.TableServiceEntity;
import com.microsoft.azure.storage.table.TableServiceException;


/**
 * Abstract class to interact with Azure Storage Table API. This classs exposes the following API:
 *
 * <ul>
 *     <li>ddl(): create/drop table operations</li>
 *     <li>crud(): CRUD operations</li>
 *     <li>partitionQuery(): query data by partition key. Filtering on row keys is possible</li>
 *     <li>filterQuery(): query data using filters.
 *      <br/>
 *      <strong>WARNING: filter query implies FULL TABLE SCAN !!! Use with extreme care</strong>
 *     </li>
 * </ul>
 * @param <T>
 */

public class AzureTableDao<T extends TableServiceEntity> {

    private static final String PARTITION_KEY = "PartitionKey";
    private static final String ROW_KEY = "RowKey";
    private static final int DEFAULT_LIMIT = 1000;
    private final Class<T> entityClass;
    private final CloudTable cloudTable;
    private final OperationContext opContext = new OperationContext();
    private final TableRequestOptions requestOptions = new TableRequestOptions();

    public AzureTableDao(Class<T> entityClass, String storageConnectionString, String tableName)  {
        this.entityClass = entityClass;
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudTableClient tableClient = storageAccount.createCloudTableClient();
            this.cloudTable = tableClient.getTableReference(tableName);
//            if (log.isDebugEnabled() || log.isTraceEnabled()) {
//                opContext.setLoggingEnabled(true);
//                opContext.setLogger(log);
//            }
            requestOptions.setLocationMode(LocationMode.PRIMARY_THEN_SECONDARY);
            requestOptions.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);
            requestOptions.setRetryPolicyFactory(new RetryExponentialRetry());

        } catch (URISyntaxException|InvalidKeyException|StorageException  e) {
            throw new RuntimeException(format("Invalid connection string : '%s'", storageConnectionString), e);
        }
    }

    /**
     * Return the DDL API, which exposes the following operations:
     * <ul>
     *     <li>createTableIfNotExists(): self-explanatory</li>
     *     <li>dropTableIfExists(): self-explanatory</li>
     * </ul>
     * @return DDL API
     */
    public DDL ddl() {
        return new DDL();
    }

    /**
     * Return the CRUD API, which exposes the following operations:
     * <ul>
     *     <li>insertOrReplace(): Insert the current entity if it does not exist or replace the existing entity</li>
     *     <li>insertOrMerge(): Insert the current entity if it does not exist or merge with the existing entity</li>
     *     <li>findById(): Find the entity by its composite id (PartitionKey, RowKey)</li>
     *     <li>deleteById(): Delete the entity by its composite id (PartitionKey, RowKey)
     *       <br/>
     *       <em>Note: this is implemented as a sequence of findById() followed by delete(T entity).
     *       Thus no transactional or atomicity is guaranteed </em>
     *     </li>
     * </ul>
     * <br/>
     * Remark: The semantics of <em>replace()</em> and <em>merge()</em> is different but very little.
     * <br/>
     * <br/>
     * Given an existing record <pre>{name: "John", "age": 32, country: "USA"}</pre>
     * <br/>
     * A merge operation with <pre>{name: "John", country: "United States", job: "Developer"}</pre>
     * would produce <pre>{name: "John", "age": 32, country: "United States", job: "Developer"}</pre>
     * Original <em>name</em> and <em>age</em> values have been kept.
     * <em>country</em> value has been updated to the latest value "United States" (Last Write Win semantics for Azure Storage Table).
     * <em>job</em> value is just added since it does not exist before
     * <br/>
     * <br/>
     * A replace operation with <pre>{name: "John", country: "United States"}</pre>
     * would produce <pre>{name: "John", country: "United States"}</pre>.
     * It basically replaces all existing data
     *
     * @return CRUD API
     */
    public CRUD crud() {
        return new CRUD();
    }

    /**
     * Query the table by partition key. Optionally filters can be added on row keys:
     * <ul>
     *     <li>from row key (inclusive or exclusive)</li>
     *     <li>up to row key (inclusive or exclusive)</li>
     *     <li>from row key (inclusive or exclusive) and up to row key (inclusive or exclusive)</li>
     * </ul>
     * <br/>
     * Optionally you can limit the query to return only top <em>limit</em> rows
     * and you can fetch only some columns instead of all using <em>select(String ... columns)</em>
     * <br/>
     * <br/>
     * Please note that the columns <em>PartitionKey</em>,<em>RowKey</em> and <em>Timestamp</em>
     * are <strong>always fetched</strong> no matter the values you set in <em>select(String ... columns)</em>
     * @param partitionKey the partition key
     * @return the Query API
     */
    public PartitionQuery partitionQuery(String partitionKey) {
        return new PartitionQuery(partitionKey);
    }

    /**
     * Query the table by filtering on non primary keys.
     * <br/>
     * If multiple filters are provided, they are combined using an <strong>AND</strong> logic
     * <br/>
     * <strong>WARNING: filter query implies FULL TABLE SCAN !!! Use with extreme care</strong>
     * @param filters the filters to apply on columns
     * @return FilterQuery API
     * @throws StorageException
     */
    public FilterQuery filterQuery(GenericFilter... filters)  {
        Validator.validateTrue(filters.length > 0, "Please provide at least one filter for filterQuery");
        List<GenericFilter> filtersList = Arrays.asList(filters);
        return new FilterQuery(filtersList);
    }

    /**
     * Full scan the table page by page
     * <br/>
     * You can either only fetch a single page by providing a large enough page size with:
     * <br/>
     * <pre class="code"><code class="java">
     *     List<Entity> singlePage = tableDao.fullScanQuery().pageSize(2).listSinglePage();
     * </code></pre>
     * <br/>
     * or fetch pages by pages by providing a continuation token and specifying the page size
     * for each iteration
     * <br/>
     * <pre class="code"><code class="java">
     *     //First page
     *     Pair<List<Entity>, ResultContinuation> pair = tableDao.fullScanQuery().pageSize(2).listFirstPage();
     *     List<Entity> firstPage = pair.getKey();
     *     ResultContinuation continuationToken = pair.getValue();
     *
     *     ... //Process first page
     *
     *     //Next page
     *     Pair<List<Entity>, ResultContinuation> nextPair = tableDao.fullScanQuery().pageSize(10).listNextPage(continuationToken);
     *     ...
     *
     * </code></pre>
     * @return
     */
    public FullScanQuery fullScanQuery() {
        return new FullScanQuery();
    }

    /**
     * DDL API, which exposes the following operations:
     * <ul>
     *     <li>createTableIfNotExists(): self-explanatory</li>
     *     <li>dropTableIfExists(): self-explanatory</li>
     * </ul>
     * @return
     */
    public class DDL {

        /**
         * CREATE TABLE IF NOT EXISTS
         */
        public void createTableIfNotExists() {
            try {
                cloudTable.createIfNotExists(requestOptions, opContext);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * DROP TABLE IF EXISTS
         */
        public void dropTableIfExists() {
            try {
                cloudTable.deleteIfExists(requestOptions, opContext);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * CRUD API, which exposes the following operations:
     * <ul>
     *     <li>insertOrReplace(): Insert the current entity if it does not exist or replace the existing entity</li>
     *     <li>insertOrMerge(): Insert the current entity if it does not exist or merge with the existing entity</li>
     *     <li>findById(): Find the entity by its composite id (PartitionKey, RowKey)</li>
     *     <li>deleteById(): Delete the entity by its composite id (PartitionKey, RowKey)
     *       <br/>
     *       <em>Note: this is implemented as a sequence of findById() followed by delete(T entity).
     *       Thus no transactional or atomicity is guaranteed </em>
     *     </li>
     * </ul>
     * <br/>
     * Remark: The semantics of <em>replace()</em> and <em>merge()</em> is different but very little.
     * <br/>
     * <br/>
     * Given an existing record <pre>{name: "John", "age": 32, country: "USA"}</pre>
     * <br/>
     * A merge operation with <pre>{name: "John", country: "United States", job: "Developer"}</pre>
     * would produce <pre>{name: "John", "age": 32, country: "United States", job: "Developer"}</pre>
     * Original <em>name</em> and <em>age</em> values have been kept.
     * <em>country</em> value has been updated to the latest value "United States" (Last Write Win semantics for Azure Storage Table).
     * <em>job</em> value is just added since it does not exist before
     * <br/>
     * <br/>
     * A replace operation with <pre>{name: "John", country: "United States"}</pre>
     * would produce <pre>{name: "John", country: "United States"}</pre>.
     * It basically replaces all existing data
     *
     */
    public class CRUD {
        /**
         * Insert the current entity if it does not exist or replace the existing entity
         * <br/>
         * <br/>
         * Given an existing record <pre>{name: "John", "age": 32, country: "USA"}</pre>
         * <br/>
         * A replace operation with <pre>{name: "John", country: "United States"}</pre>
         * would produce <pre>{name: "John", country: "United States"}</pre>
         * It basically replaces all existing data.
         * Please note that the <em>age</em>value that is not present in the update
         * is now deleted
         * @param entity then entity to be inserted or replaced
         */
        public void insertOrReplace(T entity) {
            try {
                cloudTable.execute(TableOperation.insertOrReplace(entity), requestOptions, opContext);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * <strong>Atomically</strong> insert the current entity
         * <br/>
         * <br/>
         * @param entity then entity to be inserted
         */
        public void insertIfNotExists(T entity) {
            try {
                cloudTable.execute(TableOperation.insert(entity), requestOptions, opContext);
            } catch (TableServiceException tableServiceException) {
                if (tableServiceException.getErrorCode().equalsIgnoreCase("EntityAlreadyExists")) {
                    String errMsg = format("Entity with partition key '%s' and row key '%s' already exists",
                            entity.getPartitionKey(), entity.getRowKey());
                    throw new IllegalStateException(errMsg);
                } else {
                    throw new RuntimeException(tableServiceException);
                }
            } catch (StorageException e) {
                throw new RuntimeException(e.getErrorCode());
            }
        }

        /**
         * Insert the current entity if it does not exist or merge with the existing entity
         * <br/>
         * <br/>
         * Given an existing record <pre>{name: "John", "age": 32, country: "USA"}</pre>
         * <br/>
         * A merge operation with <pre>{name: "John", country: "United States", job: "Developer"}</pre>
         * would produce <pre>{name: "John", "age": 32, country: "United States", job: "Developer"}</pre>
         * Original <em>name</em> and <em>age</em> values have been kept.
         * <em>country</em> value has been updated to the latest value "United States" (Last Write Win semantics for Azure Storage Table).
         * <em>job</em> value is just added since it does not exist before
         * @param entity the entity to be inserted or merged
         */
        public void insertOrMerge(T entity) {
            try {
                cloudTable.execute(TableOperation.insertOrMerge(entity), requestOptions, opContext);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Update <strong>ATOMICALLY</strong> the current entity
         * <br/>
         * <br/>
         * @param entity the entity to be updated
         */
        public void atomicUpdate(T entity) {
            try {
                cloudTable.execute(TableOperation.replace(entity), requestOptions, opContext);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Find the entity by its composite id (PartitionKey, RowKey)
         * @param partitionKey the partition key
         * @param rowKey the row key
         * @return the found entity or null
         */
        public T findById(String partitionKey, String rowKey) {
            try {
                TableOperation findById = TableOperation.retrieve(partitionKey, rowKey, entityClass);
                return (T)cloudTable.execute(findById, requestOptions, opContext).getResultAsType();
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Delete the entity by its composite id (PartitionKey, RowKey)
         * <br/>
         * <em>Note: this is implemented as a sequence of findById() followed by delete(T entity).
         * Thus no transactional or atomicity is guaranteed </em>
         * @param partitionKey the partition key
         * @param rowKey the row key
         */
        public void deleteById(String partitionKey, String rowKey) {
            try {
                T found = findById(partitionKey, rowKey);
                if (found != null) {
                    cloudTable.execute(TableOperation.delete(found), requestOptions, opContext);
                } else {
                    //No op
                }
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Delete existing entity
         * @param entity existing entity
         */
        public void delete(T entity) {
            try {
                cloudTable.execute(TableOperation.delete(entity), requestOptions, opContext);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * List all entities, using internal pagination
         * @return entities
         */
        public Iterable<T> findAll() {
            TableQuery<T> query = TableQuery.from(entityClass).take(DEFAULT_LIMIT);
            return new QueryResultIterable(query);
        }
    }

    /**
     * API to query the table by partition key. Optionally filters can be added on row keys:
     * <ul>
     *     <li>from row key (inclusive or exclusive)</li>
     *     <li>up to row key (inclusive or exclusive)</li>
     *     <li>from row key (inclusive or exclusive) and up to row key (inclusive or exclusive)</li>
     * </ul>
     * <br/>
     * Optionally you can limit the query to return only top <em>limit</em> rows
     * and you can fetch only some columns instead of all using <em>select(String ... columns)</em>
     * <br/>
     * Please note that the columns <em>PartitionKey</em>,<em>RowKey</em> and <em>Timestamp</em>
     * are <strong>always fetched</strong> no matter the values you set in <em>select(String ... columns)</em>
     */
    public class PartitionQuery {
        private final String partitionFilter;
        private int limit = DEFAULT_LIMIT;
        private String[] columnsToFetch = new String[0];

        public PartitionQuery(String partitionKey) {
            this.partitionFilter = generateFilterCondition(PARTITION_KEY, EQUAL, partitionKey);
        }

        /**
         * Define the columns to be fetched by this query
         * <br/>
         * <br/>
         * Please note that the columns <em>PartitionKey</em>,<em>RowKey</em> and <em>Timestamp</em>
         * are <strong>always fetched</strong> no matter the values you set in <em>select(String ... columns)</em>
         * <br/>
         * <br/>
         * <strong>Please note that all columns in Azure Storage Table are ALWAYS capitalized.
         *  <br/>
         *  If you define a property "value" in your Java bean, the corresponding column will be "Value"
         * </strong>
         *  <br/>
         *  <br/>
         *  Consequently, you should call this method as <strong><em>select("Value")</em></strong>
         *  and NOT <strong><em>select("value")</em></strong>
         *  <br/>
         *  <br/>
         *  Columns that are not fetched will be set to <strong>null</strong>
         *  when the returned rows are serialized to entities
         * @param columns CAPITALIZED columns to be fetched
         * @return
         */
        public PartitionQuery select(String ... columns) {
            validateSelectColumns(columns);
            this.columnsToFetch = columns;
            return this;
        }

        /**
         * Set the number of rows to be returned
         * @param limit number of rows to be returned
         * @return
         */
        public PartitionQuery limit(int limit) {
            Validator.validateTrue(limit > 0, "The provided limit should be strictly positive");
            this.limit = limit;
            return this;
        }

        /**
         * Return the list of matching rows and serialized them as entities
         * @return
         * @throws StorageException
         */
        public List<T> list(){
            TableQuery<T> partitionScanQuery = TableQuery.from(entityClass).where(this.partitionFilter)
                    .take(limit);
            return getResults(applySelect(partitionScanQuery, columnsToFetch));
        }

        /**
         * Scan the partition from the given RowKey
         * @param fromRowKey from given RowKey
         * @return
         */
        public From from(String fromRowKey) {
            return new From(fromRowKey, columnsToFetch);
        }

        /**
         * Scan the partition up to the given RowKey
         * @param toRowKey up to given RowKey
         * @return
         */
        public To upTo(String toRowKey) {
            return new To(toRowKey, columnsToFetch);
        }

        public class From {
            private final String fromRowKey;
            private final String[] columnsToFetch;
            private int limit = DEFAULT_LIMIT;
            private String relation = GREATER_THAN_OR_EQUAL;

            public From(String fromRowKey, String[] columnsToFetch) {
                this.fromRowKey = fromRowKey;
                this.columnsToFetch = columnsToFetch;
            }

            /**
             * Scan the partition from the given RowKey INCLUSIVE
             * @return
             */
            public From inclusive() {
                this.relation = GREATER_THAN_OR_EQUAL;
                return this;
            }

            /**
             * Scan the partition from the given RowKey EXCLUSIVE
             * @return
             */
            public From exclusive() {
                this.relation = GREATER_THAN;
                return this;
            }

            /**
             * Scan the partition up to the given RowKey
             * @param toRowKey up to given RowKey
             * @return
             */
            public FromUpTo upTo(String toRowKey) {
                return new FromUpTo(fromRowKey, toRowKey, relation, columnsToFetch);
            }

            /**
             * Set the number of rows to be returned
             * @param limit number of rows to be returned
             * @return
             */
            public From limit(int limit) {
                Validator.validateTrue(limit > 0, "The provided limit should be strictly positive");
                this.limit = limit;
                return this;
            }

            /**
             * Return the list of matching rows and serialized them as entities
             * @return
             * @throws StorageException
             */
            public List<T> list(){

                TableQuery<T> rangeQuery = TableQuery.from(entityClass).where(
                        combineFilters(
                                PartitionQuery.this.partitionFilter,
                                Operators.AND,
                                generateFilterCondition(ROW_KEY, relation, fromRowKey)))
                        .take(limit);
                return getResults(applySelect(rangeQuery, columnsToFetch));
            }
        }

        public class To {
            private final String toRowKey;
            private int limit = DEFAULT_LIMIT;
            private final String[] columnsToFetch;
            private String relation = LESS_THAN_OR_EQUAL;

            public To(String toRowKey, String[] columnsToFetch) {
                this.toRowKey = toRowKey;
                this.columnsToFetch = columnsToFetch;
            }

            /**
             * Scan the partition up to the given RowKey INCLUSIVE
             * @return
             */
            public To inclusive() {
                this.relation = LESS_THAN_OR_EQUAL;
                return this;
            }

            /**
             * Scan the partition up to the given RowKey EXCLUSIVE
             * @return
             */
            public To exclusive() {
                this.relation = LESS_THAN;
                return this;
            }

            /**
             * Set the number of rows to be returned
             * @param limit number of rows to be returned
             * @return
             */
            public To limit(int limit) {
                Validator.validateTrue(limit > 0, "The provided limit should be strictly positive");
                this.limit = limit;
                return this;
            }

            /**
             * Return the list of matching rows and serialized them as entities
             * @return
             * @throws StorageException
             */
            public List<T> list(){
                TableQuery<T> rangeQuery = TableQuery.from(entityClass).where(
                        combineFilters(
                                PartitionQuery.this.partitionFilter,
                                Operators.AND,
                                generateFilterCondition(ROW_KEY, relation, toRowKey)))
                        .take(limit);
                return getResults(applySelect(rangeQuery, columnsToFetch));
            }
        }

        public class FromUpTo {
            private final String fromRowKey;
            private final String toRowKey;
            private final String fromRelation;
            private int limit = DEFAULT_LIMIT;
            private final String[] columnsToFetch;
            private String toRelation = LESS_THAN_OR_EQUAL;

            public FromUpTo(String fromRowKey, String toRowKey, String fromRelation, String[] columnsToFetch) {
                this.fromRowKey = fromRowKey;
                this.toRowKey = toRowKey;
                this.fromRelation = fromRelation;
                this.columnsToFetch = columnsToFetch;
            }

            /**
             * Scan the partition up to the given RowKey INCLUSIVE
             * @return
             */
            public FromUpTo inclusive() {
                this.toRelation = LESS_THAN_OR_EQUAL;
                return this;
            }

            /**
             * Scan the partition up to the given RowKey EXCLUSIVE
             * @return
             */
            public FromUpTo exclusive() {
                this.toRelation = LESS_THAN;
                return this;
            }

            /**
             * Set the number of rows to be returned
             * @param limit number of rows to be returned
             * @return
             */
            public FromUpTo limit(int limit) {
                Validator.validateTrue(limit > 0, "The provided limit should be strictly positive");
                this.limit = limit;
                return this;
            }

            /**
             * Return the list of matching rows and serialized them as entities
             * @return
             * @throws StorageException
             */
            public List<T> list(){
                TableQuery<T> rangeQuery = TableQuery.from(entityClass).where(
                        combineFilters(
                                PartitionQuery.this.partitionFilter,
                                Operators.AND,
                                combineFilters(
                                        generateFilterCondition(ROW_KEY, fromRelation, fromRowKey),
                                        Operators.AND,
                                        generateFilterCondition(ROW_KEY, toRelation, toRowKey))))
                        .take(limit);
                return getResults(applySelect(rangeQuery, columnsToFetch));
            }
        }
    }

    /**
     * API for querying the table by filtering on non primary keys.
     * <br/>
     * If multiple filters are provided, they are combined using an <strong>AND</strong> logic
     * <br/>
     * <strong>WARNING: filter query implies FULL TABLE SCAN !!! Use with extreme care</strong>
     */
    public class FilterQuery {
        private final List<GenericFilter> filters;

        private int limit = DEFAULT_LIMIT;
        private String[] columnsToFetch = new String[0];
        public FilterQuery(List<GenericFilter> filters) {
            this.filters = filters;
        }

        /**
         * Set the number of rows to be returned
         * @param limit number of rows to be returned
         * @return
         */
        public FilterQuery limit(int limit) {
            Validator.validateTrue(limit > 0, "The provided limit should be strictly positive");
            this.limit = limit;
            return this;
        }

        /**
         * Define the columns to be fetched by this query
         * <br/>
         * <br/>
         * Please note that the columns <em>PartitionKey</em>,<em>RowKey</em> and <em>Timestamp</em>
         * are <strong>always fetched</strong> no matter the values you set in <em>select(String ... columns)</em>
         * <br/>
         * <br/>
         * <strong>Please note that all columns in Azure Storage Table are ALWAYS capitalized.
         *  <br/>
         *  If you define a property "value" in your Java bean, the corresponding column will be "Value"
         * </strong>
         *  <br/>
         *  <br/>
         *  Consequently, you should call this method as <strong><em>select("Value")</em></strong>
         *  and NOT <strong><em>select("value")</em></strong>
         *  <br/>
         *  <br/>
         *  Columns that are not fetched will be set to <strong>null</strong>
         *  when the returned rows are serialized to entities
         * @param columns CAPITALIZED columns to be fetched
         * @return
         */
        public FilterQuery select(String ... columns) {
            validateSelectColumns(columns);
            this.columnsToFetch = columns;
            return this;
        }

        /**
         * Return the list of matching rows and serialized them as entities
         * @return
         * @throws StorageException
         */
        public List<T> list() {
            String filtersExpression = filters
                    .stream()
                    .map(GenericFilter::filter)
                    .collect(Collectors.joining(" and "));
            TableQuery<T> query = TableQuery.from(entityClass)
                    .where(filtersExpression)
                    .take(limit);
            return getResults(applySelect(query, columnsToFetch));
        }

    }

    /**
     * API to full scan the table by pages
     * <br/>
     * <strong>WARNING: full scanning the table can be very slow !!</strong>
     */
    public class FullScanQuery {
        private int pageSize = DEFAULT_LIMIT;
        private String[] columnsToFetch = new String[0];

        /**
         * Set the number of rows to be returned <strong>for each page</strong>
         * @param pageSize number of rows to be returned
         * @return
         */
        public FullScanQuery pageSize(int pageSize) {
            Validator.validateTrue(pageSize > 0, "The provided pageSize should be strictly positive");
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Define the columns to be fetched by this query
         * <br/>
         * <br/>
         * Please note that the columns <em>PartitionKey</em>,<em>RowKey</em> and <em>Timestamp</em>
         * are <strong>always fetched</strong> no matter the values you set in <em>select(String ... columns)</em>
         * <br/>
         * <br/>
         * <strong>Please note that all columns in Azure Storage Table are ALWAYS capitalized.
         *  <br/>
         *  If you define a property "value" in your Java bean, the corresponding column will be "Value"
         * </strong>
         *  <br/>
         *  <br/>
         *  Consequently, you should call this method as <strong><em>select("Value")</em></strong>
         *  and NOT <strong><em>select("value")</em></strong>
         *  <br/>
         *  <br/>
         *  Columns that are not fetched will be set to <strong>null</strong>
         *  when the returned rows are serialized to entities
         * @param columns CAPITALIZED columns to be fetched
         * @return
         */
        public FullScanQuery select(String ... columns) {
            validateSelectColumns(columns);
            this.columnsToFetch = columns;
            return this;
        }

        /**
         * Return the list of first result page and serialized them as entities
         * @return
         * @throws StorageException
         */
        public List<T> listSinglePage() {
            TableQuery<T> query = TableQuery.from(entityClass)
                    .take(pageSize);
            return getResultsWithToken(applySelect(query, columnsToFetch), null).getKey();
        }

        /**
         * Return the list of first result page with continuation token
         * @return
         * @throws StorageException
         */
        public Pair<List<T>,ResultContinuation> listFirstPage() {
            TableQuery<T> query = TableQuery.from(entityClass)
                    .take(pageSize);
            return getResultsWithToken(applySelect(query, columnsToFetch), null);
        }

        /**
         * Return the list of next result page with continuation token
         * @param continuationToken continuation token to fetch next page
         * @return
         * @throws StorageException
         */
        public Pair<List<T>,ResultContinuation> listNextPage(ResultContinuation continuationToken) {
            TableQuery<T> query = TableQuery.from(entityClass)
                    .take(pageSize);
            return getResultsWithToken(applySelect(query, columnsToFetch), continuationToken);
        }

        private Pair<List<T>,ResultContinuation> getResultsWithToken(TableQuery<T> rangeQuery, ResultContinuation continuationToken){
            try {
                ResultSegment<T> resultSegment = cloudTable.executeSegmented(rangeQuery, continuationToken, requestOptions, opContext);
                return Pair.of(resultSegment.getResults(), resultSegment.getContinuationToken());

            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * API to full scan the table by pages transparently as a ResultSet
     * adaptor from azure pagination query cloudTable.executeSegmented(..) to java.util.Iterable<T>
     * <br/>
     * <strong>WARNING: full scanning the table can be very slow !!</strong>
     */
    public class QueryResultIterable implements Iterable<T> {
        private final TableQuery<T> query;

        public QueryResultIterable(TableQuery<T> query) {
            this.query = query;
        }

        @Override
        public Iterator<T> iterator() {
            return new QueryResultIterator(query);
        }
    }

    /**
     * adaptor from azure pagination query cloudTable.executeSegmented(..) to java.util.Iterator<T>
     */
    public class QueryResultIterator implements Iterator<T> {
        private final TableQuery<T> query;
        private Iterator<T> currPageIterator;
        private ResultContinuation continuationToken;

        public QueryResultIterator(TableQuery<T> query) {
            this.query = query;
            ResultSegment<T> resultSegment;
            try {
                resultSegment = cloudTable.executeSegmented(query, continuationToken, requestOptions, opContext);
            } catch (StorageException ex) {
                throw new RuntimeException("Failed table.executeSegmented", ex);
            }
            currPageIterator = resultSegment.getResults().iterator();
            continuationToken = resultSegment.getContinuationToken();
        }

        @Override
        public boolean hasNext() {
            return currPageIterator.hasNext() || continuationToken != null;
        }

        @Override
        public T next() {
            if (! currPageIterator.hasNext() && continuationToken != null) {
                ResultSegment<T> resultSegment;
                try {
                    resultSegment = cloudTable.executeSegmented(query, continuationToken, requestOptions, opContext);
                } catch (StorageException ex) {
                    throw new RuntimeException("Failed table.executeSegmented", ex);
                }
                currPageIterator = resultSegment.getResults().iterator();
                continuationToken = resultSegment.getContinuationToken();
            }

            T res = currPageIterator.next();
            return res;
        }

    }


    private void validateSelectColumns(String[] columns) {
        List<String> columnsToFetch = Arrays.asList(columns);
        Validator.validateTrue(!columnsToFetch.isEmpty(), "You should provide at least one column to select");
        long emptyColumns = columnsToFetch
                .stream()
                .filter(column -> StringUtils.isBlank(column))
                .count();
        Validator.validateTrue(emptyColumns <= 0, "The provided columns to be fetched should not be blank");
    }

    private TableQuery<T> applySelect(TableQuery<T> rangeQuery, String[] columnsToFetch) {
        if (ArrayUtils.isNotEmpty(columnsToFetch)) {
            rangeQuery = rangeQuery.select(columnsToFetch);
        }
        return rangeQuery;
    }

    private ArrayList<T> getResults(TableQuery<T> rangeQuery){
        try {
            return cloudTable.executeSegmented(rangeQuery, null, requestOptions, opContext)
                    .getResults();
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generic filter on column.
     * <br/>
     * Supported operators are:
     * <ul>
     *     <li>{@link Comp#eq}</li>
     *     <li>{@link Comp#ge}</li>
     *     <li>{@link Comp#gt}</li>
     *     <li>{@link Comp#lt}</li>
     *     <li>{@link Comp#le}</li>
     * </ul>
     * Supported data types are:
     * <ul>
     *     <li>String</li>
     *     <li>boolean/Boolean</li>
     *     <li>byte[]</li>
     *     <li>Byte[]</li>
     *     <li>java.util.Date</li>
     *     <li>double/Double</li>
     *     <li>int/Integer</li>
     *     <li>long/Long</li>
     *     <li>java.util.UUID</li>
     * </ul>
     */
    public static class GenericFilter {
        public final String field;
        public final Comp comparator;
        public final Object value;
        public final EdmType dataType;

        public GenericFilter(String field, Comp comparator, String value) {
            this.field = capitalize(field);
            this.comparator = comparator;
            this.value = value;
            this.dataType = EdmType.STRING;
        }

        public GenericFilter(String field, Comp comparator, boolean value) {
            this.field = capitalize(field);
            this.comparator = comparator;
            this.value = value;
            this.dataType = EdmType.BOOLEAN;
        }

        public GenericFilter(String field, Comp comparator, byte[] value) {
            this.field = capitalize(field);
            this.comparator = comparator;
            this.value = value;
            this.dataType = EdmType.BINARY;
        }

        public GenericFilter(String field, Comp comparator, Byte[] value) {
            this.field = capitalize(field);
            this.comparator = comparator;
            this.value = value;
            this.dataType = EdmType.BINARY;
        }

        public GenericFilter(String field, Comp comparator, Date value) {
            this.field = capitalize(field);
            this.comparator = comparator;
            this.value = value;
            this.dataType = EdmType.DATE_TIME;
        }

        public GenericFilter(String field, Comp comparator, double value) {
            this.field = capitalize(field);
            this.comparator = comparator;
            this.value = value;
            this.dataType = EdmType.DOUBLE;
        }

        public GenericFilter(String field, Comp comparator, int value) {
            this.field = capitalize(field);
            this.comparator = comparator;
            this.value = value;
            this.dataType = EdmType.INT32;
        }

        public GenericFilter(String field, Comp comparator, long value) {
            this.field = capitalize(field);
            this.comparator = comparator;
            this.value = value;
            this.dataType = EdmType.INT64;
        }

        public GenericFilter(String field, Comp comparator, UUID value) {
            this.field = capitalize(field);
            this.comparator = comparator;
            this.value = value;
            this.dataType = EdmType.GUID;
        }

        /**
         * Generate the filter string for query
         * @return
         */
        public String filter() {
            switch (dataType) {
                case BINARY:
                    if (value instanceof byte[]) {
                        return generateFilterCondition(field, comparator.name(), (byte[]) value);
                    } else {
                        return generateFilterCondition(field, comparator.name(), (Byte[]) value);
                    }
                case BOOLEAN:
                    return generateFilterCondition(field, comparator.name(), (boolean) value);
                case DATE_TIME:
                    return generateFilterCondition(field, comparator.name(), (Date) value);
                case DOUBLE:
                    return generateFilterCondition(field, comparator.name(), (double) value);
                case GUID:
                    return generateFilterCondition(field, comparator.name(), (UUID) value);
                case INT32:
                    return generateFilterCondition(field, comparator.name(), (int) value);
                case INT64:
                    return generateFilterCondition(field, comparator.name(), (long) value);
                case STRING:
                    return generateFilterCondition(field, comparator.name(), (String) value);
                default:
                    throw new IllegalArgumentException(format("Unknown type for value '%s'", value));
            }
        }

    }

    /**
     * Enum defining comparator for filtering on columns values
     */
    public enum Comp {
        eq,
        gt,
        lt,
        ge,
        le
    }


}
