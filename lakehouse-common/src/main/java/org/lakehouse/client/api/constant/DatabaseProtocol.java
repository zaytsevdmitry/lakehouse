/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.client.api.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents the list of supported database protocols and engines.
 * <p>
 * <b>NOTE:</b> The presence of an engine in this enumeration only enables it to be selected 
 * or specified within the system configuration metadata. It does not guarantee mandatory out-of-the-box 
 * feature support by the execution mechanism.
 * </p>
 * <p>
 * Depending on the chosen target system:
 * <ul>
 *   <li>Some databases require providing the corresponding third-party JDBC/NoSQL driver dependency at runtime.</li>
 *   <li>Other databases may require a completely custom or full-fledged client implementation to be built into the runner engine.</li>
 * </ul>
 * </p>
 */
public enum DatabaseProtocol {
    
    // --- CIS & RU Specific Databases & Forks ---
    YDB("ydb", true, 2135),                // Yandex Database (gRPC/s protocol, but has official JDBC driver `jdbc:ydb:`)
    TARANTOOL("tarantool", false, 3301),   // VK Tarantool In-Memory (typically `tarantool://`, `jdbc:tarantool:` or via CRUD)
    ARENADATA_DB("postgresql", true, 5432),// ADB based on Greenplum. Compatible with Postgres driver
    ARENADATA_CH("clickhouse", true, 8123),// ADCH based on ClickHouse. Uses HTTP (8123) or Native (9000)
    ARENADATA_HD("hive2", true, 10000),    // ADHD based on Hadoop Ecosystem (Hive/Spark)
    PROSTRE_PRO("postgresql", true, 5432), // Postgres Pro (fully compatible with postgresql prefix)
    ASTRO_DB("astrodb", false, 0),         // Distributed cloud ecosystems

    // --- Classical Relational Databases (RDBMS) ---
    POSTGRESQL("postgresql", true, 5432),
    MYSQL("mysql", true, 3306),
    ORACLE("oracle", true, 1521),
    SQL_SERVER("sqlserver", true, 1433),
    MARIADB("mariadb", true, 3306),
    DB2("db2", true, 50000),
    FIREBIRD("firebird", true, 3050),
    H2("h2", true, 9092),
    SQLITE("sqlite", true, 0),             // Embedded (no port required)

    // --- Analytical / Big Data (OLAP) ---
    HIVE2("hive2", true, 10000),           // Apache Hive, Spark Thrift Server, Cloudera Impala
    CLICKHOUSE("clickhouse", true, 8123),
    GREENPLUM("greenplum", true, 5432),
    TRINO("trino", true, 8080),            // Current Presto fork
    PRESTO("presto", true, 8080),
    SNOWFLAKE("snowflake", true, 443),
    DATABRICKS("databricks", true, 443),
    BIGQUERY("bigquery", true, 443),

    // --- NoSQL / Document / Key-Value ---
    MONGO("mongodb", false, 27017),         // Native prefix `mongodb://` or `mongodb+srv://`
    REDIS("redis", false, 6379),            // Native prefix `redis://`
    CASSANDRA("cassandra", true, 9042),     // Native Cassandra port (supports third-party JDBC wrappers)
    ELASTICSEARCH("elasticsearch", false, 9200),

    // --- Time Series & Graph Databases ---
    INFLUXDB("influxdb", false, 8086),
    NEO4J("neo4j", false, 7687),         // Protocols `bolt://` or `neo4j://`
    S3("s3",false,9001);
    private final String protocolPrefix;
    private final boolean jdbcStandard;
    private final int defaultPort;

    DatabaseProtocol(String protocolPrefix, boolean jdbcStandard, int defaultPort) {
        this.protocolPrefix = protocolPrefix;
        this.jdbcStandard = jdbcStandard;
        this.defaultPort = defaultPort;
    }

    public String getProtocolPrefix() {
        return this.protocolPrefix;
    }

    public boolean isJdbcStandard() {
        return this.jdbcStandard;
    }

    public int getDefaultPort() {
        return this.defaultPort;
    }

    /**
     * Allows a factory or execution mechanism to dynamically build 
     * a base connection string based on metadata.
     */
    public  String buildConnectionStringTemplate(String host, int port, String database) {
        int finalPort = (port <= 0) ? this.defaultPort : port;
        String hostAndPort = finalPort > 0 ? host + ":" + finalPort : host;
        
        if (this.jdbcStandard) {
            return String.format("jdbc:%s://%s/%s", this.protocolPrefix, hostAndPort, database);
        } else {
            return String.format("%s://%s/%s", this.protocolPrefix, hostAndPort, database);
        }
    }

    /**
     * Parses a raw incoming connection string for automatic mapping.
     */
    public static DatabaseProtocol fromConnectionString(String connectionString) {
        if (connectionString == null || connectionString.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection string cannot be null or empty");
        }
        
        String lowerCaseStr = connectionString.toLowerCase();
        for (DatabaseProtocol db : DatabaseProtocol.values()) {
            // Looks for sub-protocol occurrences: e.g., ":postgresql:" or "mongodb://"
            if (lowerCaseStr.contains(":" + db.getProtocolPrefix() + ":") || 
                lowerCaseStr.contains(":" + db.getProtocolPrefix() + "/") || 
                lowerCaseStr.contains(db.getProtocolPrefix() + "://")) {
                return db;
            }
        }
        throw new IllegalArgumentException("Unknown database protocol in connection string: " + connectionString);
    }
    @JsonCreator
    public static DatabaseProtocol fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

         for (DatabaseProtocol db : DatabaseProtocol.values()) {
            if (db.name().equalsIgnoreCase(value.trim()) || db.protocolPrefix.equalsIgnoreCase(value.trim())) {
                return db;
            }
        }

        throw new IllegalArgumentException("Unknown database protocol value: " + value);
    }

}
