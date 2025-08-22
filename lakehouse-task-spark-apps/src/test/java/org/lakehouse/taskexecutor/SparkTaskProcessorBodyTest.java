package org.lakehouse.taskexecutor;

import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.body.SparkProcessorBodyFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
public class SparkTaskProcessorBodyTest {
    String config = "{\n" +
            "  \"executionModuleArgs\" : {\n" +
            "    \"spark.executor.memory\" : \"1g\",\n" +
            "    \"spark.driver.memory\" : \"1g\",\n" +
            "    \"spark.deploy.maxExecutorRetries\" : \"10\",\n" +
            "    \"spark.driver.extraJavaOptions\" : \"--add-opens java.base/sun.nio.ch=ALL-UNNAMED\",\n" +
            "    \"spark.executor.extraJavaOptions\" : \"--add-opens java.base/sun.nio.ch=ALL-UNNAMED\",\n" +
            "    \"executionBody\" : \"org.lakehouse.taskexecutor.executionmodule.body.SparkTaskProcessorBody\"\n" +
            "  },\n" +
            "  \"scripts\" : [ \"select t.id id\\n     , t.reg_date_time\\n     , c.id   as client_id\\n     , c.name as client_name\\n     , t.provider_id\\n     , t.amount\\n     , t.commission\\nfrom transaction_processing t\\njoin client_processing c\\n  on t.client_id = c.id\\n where\\n   t.reg_date_time >= timestamp '2025-01-01T00:00:00Z' and\\n   t.reg_date_time < timestamp '2025-01-02T00:00:00Z'\\n-- NB spark sql\\n\" ],\n" +
            "  \"sources\" : {\n" +
            "    \"transaction_processing\" : {\n" +
            "      \"keyName\" : \"transaction_processing\",\n" +
            "      \"project\" : \"DEMO\",\n" +
            "      \"dataStoreKeyName\" : \"processingdb\",\n" +
            "      \"fullTableName\" : \"proc.transactions\",\n" +
            "      \"sources\" : [ ],\n" +
            "      \"columnSchema\" : [ {\n" +
            "        \"name\" : \"id\",\n" +
            "        \"description\" : \"tx id\",\n" +
            "        \"dataType\" : \"serial\",\n" +
            "        \"nullable\" : false,\n" +
            "        \"order\" : 0,\n" +
            "        \"sequence\" : false\n" +
            "      }, {\n" +
            "        \"name\" : \"amount\",\n" +
            "        \"description\" : \"Amount paid by the client\",\n" +
            "        \"dataType\" : \"decimal\",\n" +
            "        \"nullable\" : false,\n" +
            "        \"order\" : null,\n" +
            "        \"sequence\" : false\n" +
            "      }, {\n" +
            "        \"name\" : \"client_id\",\n" +
            "        \"description\" : \"from client\",\n" +
            "        \"dataType\" : \"integer\",\n" +
            "        \"nullable\" : false,\n" +
            "        \"order\" : null,\n" +
            "        \"sequence\" : false\n" +
            "      }, {\n" +
            "        \"name\" : \"commission\",\n" +
            "        \"description\" : \"Commission due to us\",\n" +
            "        \"dataType\" : \"decimal\",\n" +
            "        \"nullable\" : false,\n" +
            "        \"order\" : null,\n" +
            "        \"sequence\" : false\n" +
            "      }, {\n" +
            "        \"name\" : \"provider_id\",\n" +
            "        \"description\" : \"To provider\",\n" +
            "        \"dataType\" : \"integer\",\n" +
            "        \"nullable\" : false,\n" +
            "        \"order\" : null,\n" +
            "        \"sequence\" : false\n" +
            "      }, {\n" +
            "        \"name\" : \"reg_date_time\",\n" +
            "        \"description\" : \"Transaction registration\",\n" +
            "        \"dataType\" : \"timestamp\",\n" +
            "        \"nullable\" : false,\n" +
            "        \"order\" : null,\n" +
            "        \"sequence\" : false\n" +
            "      } ],\n" +
            "      \"properties\" : {\n" +
            "        \"fetchSize\" : \"10000\"\n" +
            "      },\n" +
            "      \"scripts\" : [ {\n" +
            "        \"key\" : \"transaction_processing.sql\",\n" +
            "        \"order\" : 1\n" +
            "      } ],\n" +
            "      \"description\" : \"remote transactions table placed  in db schema proc\",\n" +
            "      \"constraints\" : [ {\n" +
            "        \"name\" : \"transaction_processing_pk\",\n" +
            "        \"type\" : \"primary\",\n" +
            "        \"columns\" : \"id\",\n" +
            "        \"enabled\" : false,\n" +
            "        \"runtimeLevelCheck\" : false,\n" +
            "        \"constructLevelCheck\" : true\n" +
            "      } ]\n" +
            "    },\n" +
            "    \"client_processing\" : {\n" +
            "      \"keyName\" : \"client_processing\",\n" +
            "      \"project\" : \"DEMO\",\n" +
            "      \"dataStoreKeyName\" : \"processingdb\",\n" +
            "      \"fullTableName\" : \"proc.client\",\n" +
            "      \"sources\" : [ ],\n" +
            "      \"columnSchema\" : [ {\n" +
            "        \"name\" : \"id\",\n" +
            "        \"description\" : \"Client id\",\n" +
            "        \"dataType\" : \"integer\",\n" +
            "        \"nullable\" : false,\n" +
            "        \"order\" : null,\n" +
            "        \"sequence\" : false\n" +
            "      }, {\n" +
            "        \"name\" : \"name\",\n" +
            "        \"description\" : \"Client name\",\n" +
            "        \"dataType\" : \"varchar(255)\",\n" +
            "        \"nullable\" : false,\n" +
            "        \"order\" : null,\n" +
            "        \"sequence\" : false\n" +
            "      }, {\n" +
            "        \"name\" : \"reg_date_time\",\n" +
            "        \"description\" : \"Client registration\",\n" +
            "        \"dataType\" : \"timestamp\",\n" +
            "        \"nullable\" : false,\n" +
            "        \"order\" : null,\n" +
            "        \"sequence\" : false\n" +
            "      } ],\n" +
            "      \"properties\" : {\n" +
            "        \"data-end-point\" : \"proc.client\",\n" +
            "        \"fetchSize\" : \"10000\"\n" +
            "      },\n" +
            "      \"scripts\" : [ {\n" +
            "        \"key\" : \"client_processing.sql\",\n" +
            "        \"order\" : null\n" +
            "      } ],\n" +
            "      \"description\" : \"remote dataset with clients\",\n" +
            "      \"constraints\" : [ {\n" +
            "        \"name\" : \"client_processing_pk\",\n" +
            "        \"type\" : \"primary\",\n" +
            "        \"columns\" : \"id\",\n" +
            "        \"enabled\" : true,\n" +
            "        \"runtimeLevelCheck\" : false,\n" +
            "        \"constructLevelCheck\" : true\n" +
            "      }, {\n" +
            "        \"name\" : \"client_processing_pk\",\n" +
            "        \"type\" : \"unique\",\n" +
            "        \"columns\" : \"name\",\n" +
            "        \"enabled\" : true,\n" +
            "        \"runtimeLevelCheck\" : false,\n" +
            "        \"constructLevelCheck\" : true\n" +
            "      } ]\n" +
            "    }\n" +
            "  },\n" +
            "  \"targetDataSet\" : {\n" +
            "    \"keyName\" : \"transaction_dds\",\n" +
            "    \"project\" : \"DEMO\",\n" +
            "    \"dataStoreKeyName\" : \"lakehousestorage\",\n" +
            "    \"fullTableName\" : \"transaction_dds\",\n" +
            "    \"sources\" : [ {\n" +
            "      \"name\" : \"client_processing\",\n" +
            "      \"properties\" : {\n" +
            "        \"fetchSize\" : \"10000\"\n" +
            "      }\n" +
            "    }, {\n" +
            "      \"name\" : \"transaction_processing\",\n" +
            "      \"properties\" : {\n" +
            "        \"fetchSize\" : \"10000\"\n" +
            "      }\n" +
            "    } ],\n" +
            "    \"columnSchema\" : [ {\n" +
            "      \"name\" : \"id\",\n" +
            "      \"description\" : \"tx id\",\n" +
            "      \"dataType\" : \"bigint\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : 0,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"amount\",\n" +
            "      \"description\" : \"Amount paid by the client\",\n" +
            "      \"dataType\" : \"decimal\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"client_id\",\n" +
            "      \"description\" : \"from client\",\n" +
            "      \"dataType\" : \"string\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"client_name\",\n" +
            "      \"description\" : \"Client name\",\n" +
            "      \"dataType\" : \"string\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"commission\",\n" +
            "      \"description\" : \"Commission due to us\",\n" +
            "      \"dataType\" : \"string\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"provider_id\",\n" +
            "      \"description\" : \"To provider\",\n" +
            "      \"dataType\" : \"string\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"reg_date_time\",\n" +
            "      \"description\" : \"Transaction registration\",\n" +
            "      \"dataType\" : \"timestamp\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    } ],\n" +
            "    \"properties\" : {\n" +
            "      \"using\" : \"iceberg\",\n" +
            "      \"location\" : \"/mytabs/transaction_dds\"\n" +
            "    },\n" +
            "    \"scripts\" : [ {\n" +
            "      \"key\" : \"transaction_dds.sql\",\n" +
            "      \"order\" : null\n" +
            "    } ],\n" +
            "    \"description\" : \"Details\",\n" +
            "    \"constraints\" : [ {\n" +
            "      \"name\" : \"transaction_dds_pk\",\n" +
            "      \"type\" : \"primary\",\n" +
            "      \"columns\" : \"id\",\n" +
            "      \"enabled\" : false,\n" +
            "      \"runtimeLevelCheck\" : false,\n" +
            "      \"constructLevelCheck\" : true\n" +
            "    } ]\n" +
            "  },\n" +
            "  \"dataStores\" : {\n" +
            "    \"lakehousestorage\" : {\n" +
            "      \"name\" : \"lakehousestorage\",\n" +
            "      \"interfaceType\" : \"filesystem\",\n" +
            "      \"vendor\" : \"localfs\",\n" +
            "      \"properties\" : { },\n" +
            "      \"driverClassName\" : null,\n" +
            "      \"description\" : \"Local datastore\",\n" +
            "      \"url\" : \"file:///tmp/lakehouse\"\n" +
            "    },\n" +
            "    \"processingdb\" : {\n" +
            "      \"name\" : \"processingdb\",\n" +
            "      \"interfaceType\" : \"jdbc\",\n" +
            "      \"vendor\" : \"postgres\",\n" +
            "      \"properties\" : {\n" +
            "        \"password\" : \"postgresPW\",\n" +
            "        \"user\" : \"postgresUser\"\n" +
            "      },\n" +
            "      \"driverClassName\" : null,\n" +
            "      \"description\" : \"Remote datastore processingdb\",\n" +
            "      \"url\" : \"jdbc:postgresql://localhost:5432/postgresDB\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"tableDefinitions\" : {\n" +
            "    \"transaction_processing\" : {\n" +
            "      \"schemaName\" : \"proc\",\n" +
            "      \"tableName\" : \"transactions\",\n" +
            "      \"fullTableName\" : \"proc.transactions\",\n" +
            "      \"columnsComaSeparated\" : \"id, amount, client_id, commission, provider_id, reg_date_time\",\n" +
            "      \"columnsDDL\" : \"id serial,amount decimal,client_id integer,commission decimal,provider_id integer,reg_date_time timestamp\",\n" +
            "      \"columnsUpdateSet\" : \"t.id = q.id\\n, t.amount = q.amount\\n, t.client_id = q.client_id\\n, t.commission = q.commission\\n, t.provider_id = q.provider_id\\n, t.reg_date_time = q.reg_date_time\\n\",\n" +
            "      \"columnsMergeInsertValues\" : \"q.id, q.amount, q.client_id, q.commission, q.provider_id, q.reg_date_time\",\n" +
            "      \"columnsMergeOn\" : \"t.id = q.id\",\n" +
            "      \"tableDDL\" : \"create table proc.transactions\\n (id serial,amount decimal,client_id integer,commission decimal,provider_id integer,reg_date_time timestamp)\\n java.util.stream.ReferencePipeline$3@64325682\",\n" +
            "      \"columnsSelectWithCast\" : \"cast( id as  serial) as id,cast( amount as  decimal) as amount,cast( client_id as  integer) as client_id,cast( commission as  decimal) as commission,cast( provider_id as  integer) as provider_id,cast( reg_date_time as  timestamp) as reg_date_time\",\n" +
            "      \"primaryKeys\" : [ \"id\" ]\n" +
            "    },\n" +
            "    \"client_processing\" : {\n" +
            "      \"schemaName\" : \"proc\",\n" +
            "      \"tableName\" : \"client\",\n" +
            "      \"fullTableName\" : \"proc.client\",\n" +
            "      \"columnsComaSeparated\" : \"id, name, reg_date_time\",\n" +
            "      \"columnsDDL\" : \"id integer,name varchar(255),reg_date_time timestamp\",\n" +
            "      \"columnsUpdateSet\" : \"t.id = q.id\\n, t.name = q.name\\n, t.reg_date_time = q.reg_date_time\\n\",\n" +
            "      \"columnsMergeInsertValues\" : \"q.id, q.name, q.reg_date_time\",\n" +
            "      \"columnsMergeOn\" : \"t.id = q.id\",\n" +
            "      \"tableDDL\" : \"create table proc.client\\n (id integer,name varchar(255),reg_date_time timestamp)\\n java.util.stream.ReferencePipeline$3@6b5b4cda\",\n" +
            "      \"columnsSelectWithCast\" : \"cast( id as  integer) as id,cast( name as  varchar(255)) as name,cast( reg_date_time as  timestamp) as reg_date_time\",\n" +
            "      \"primaryKeys\" : [ \"id\" ]\n" +
            "    },\n" +
            "    \"transaction_dds\" : {\n" +
            "      \"schemaName\" : null,\n" +
            "      \"tableName\" : \"transaction_dds\",\n" +
            "      \"fullTableName\" : \"transaction_dds\",\n" +
            "      \"columnsComaSeparated\" : \"id, amount, client_id, client_name, commission, provider_id, reg_date_time\",\n" +
            "      \"columnsDDL\" : \"id bigint,amount decimal,client_id string,client_name string,commission string,provider_id string,reg_date_time timestamp\",\n" +
            "      \"columnsUpdateSet\" : \"t.id = q.id\\n, t.amount = q.amount\\n, t.client_id = q.client_id\\n, t.client_name = q.client_name\\n, t.commission = q.commission\\n, t.provider_id = q.provider_id\\n, t.reg_date_time = q.reg_date_time\\n\",\n" +
            "      \"columnsMergeInsertValues\" : \"q.id, q.amount, q.client_id, q.client_name, q.commission, q.provider_id, q.reg_date_time\",\n" +
            "      \"columnsMergeOn\" : \"t.id = q.id\",\n" +
            "      \"tableDDL\" : \"create table transaction_dds\\n (id bigint,amount decimal,client_id string,client_name string,commission string,provider_id string,reg_date_time timestamp)\\n java.util.stream.ReferencePipeline$3@2f0c40f7\",\n" +
            "      \"columnsSelectWithCast\" : \"cast( id as  bigint) as id,cast( amount as  decimal) as amount,cast( client_id as  string) as client_id,cast( client_name as  string) as client_name,cast( commission as  string) as commission,cast( provider_id as  string) as provider_id,cast( reg_date_time as  timestamp) as reg_date_time\",\n" +
            "      \"primaryKeys\" : [ \"id\" ]\n" +
            "    }\n" +
            "  },\n" +
            "  \"dataSetDTOSet\" : [ {\n" +
            "    \"keyName\" : \"transaction_dds\",\n" +
            "    \"project\" : \"DEMO\",\n" +
            "    \"dataStoreKeyName\" : \"lakehousestorage\",\n" +
            "    \"fullTableName\" : \"transaction_dds\",\n" +
            "    \"sources\" : [ {\n" +
            "      \"name\" : \"client_processing\",\n" +
            "      \"properties\" : {\n" +
            "        \"fetchSize\" : \"10000\"\n" +
            "      }\n" +
            "    }, {\n" +
            "      \"name\" : \"transaction_processing\",\n" +
            "      \"properties\" : {\n" +
            "        \"fetchSize\" : \"10000\"\n" +
            "      }\n" +
            "    } ],\n" +
            "    \"columnSchema\" : [ {\n" +
            "      \"name\" : \"id\",\n" +
            "      \"description\" : \"tx id\",\n" +
            "      \"dataType\" : \"bigint\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : 0,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"amount\",\n" +
            "      \"description\" : \"Amount paid by the client\",\n" +
            "      \"dataType\" : \"decimal\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"client_id\",\n" +
            "      \"description\" : \"from client\",\n" +
            "      \"dataType\" : \"string\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"client_name\",\n" +
            "      \"description\" : \"Client name\",\n" +
            "      \"dataType\" : \"string\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"commission\",\n" +
            "      \"description\" : \"Commission due to us\",\n" +
            "      \"dataType\" : \"string\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"provider_id\",\n" +
            "      \"description\" : \"To provider\",\n" +
            "      \"dataType\" : \"string\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"reg_date_time\",\n" +
            "      \"description\" : \"Transaction registration\",\n" +
            "      \"dataType\" : \"timestamp\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    } ],\n" +
            "    \"properties\" : {\n" +
            "      \"using\" : \"iceberg\",\n" +
            "      \"location\" : \"/mytabs/transaction_dds\"\n" +
            "    },\n" +
            "    \"scripts\" : [ {\n" +
            "      \"key\" : \"transaction_dds.sql\",\n" +
            "      \"order\" : null\n" +
            "    } ],\n" +
            "    \"description\" : \"Details\",\n" +
            "    \"constraints\" : [ {\n" +
            "      \"name\" : \"transaction_dds_pk\",\n" +
            "      \"type\" : \"primary\",\n" +
            "      \"columns\" : \"id\",\n" +
            "      \"enabled\" : false,\n" +
            "      \"runtimeLevelCheck\" : false,\n" +
            "      \"constructLevelCheck\" : true\n" +
            "    } ]\n" +
            "  }, {\n" +
            "    \"keyName\" : \"transaction_processing\",\n" +
            "    \"project\" : \"DEMO\",\n" +
            "    \"dataStoreKeyName\" : \"processingdb\",\n" +
            "    \"fullTableName\" : \"proc.transactions\",\n" +
            "    \"sources\" : [ ],\n" +
            "    \"columnSchema\" : [ {\n" +
            "      \"name\" : \"id\",\n" +
            "      \"description\" : \"tx id\",\n" +
            "      \"dataType\" : \"serial\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : 0,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"amount\",\n" +
            "      \"description\" : \"Amount paid by the client\",\n" +
            "      \"dataType\" : \"decimal\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"client_id\",\n" +
            "      \"description\" : \"from client\",\n" +
            "      \"dataType\" : \"integer\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"commission\",\n" +
            "      \"description\" : \"Commission due to us\",\n" +
            "      \"dataType\" : \"decimal\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"provider_id\",\n" +
            "      \"description\" : \"To provider\",\n" +
            "      \"dataType\" : \"integer\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"reg_date_time\",\n" +
            "      \"description\" : \"Transaction registration\",\n" +
            "      \"dataType\" : \"timestamp\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    } ],\n" +
            "    \"properties\" : {\n" +
            "      \"fetchSize\" : \"10000\"\n" +
            "    },\n" +
            "    \"scripts\" : [ {\n" +
            "      \"key\" : \"transaction_processing.sql\",\n" +
            "      \"order\" : 1\n" +
            "    } ],\n" +
            "    \"description\" : \"remote transactions table placed  in db schema proc\",\n" +
            "    \"constraints\" : [ {\n" +
            "      \"name\" : \"transaction_processing_pk\",\n" +
            "      \"type\" : \"primary\",\n" +
            "      \"columns\" : \"id\",\n" +
            "      \"enabled\" : false,\n" +
            "      \"runtimeLevelCheck\" : false,\n" +
            "      \"constructLevelCheck\" : true\n" +
            "    } ]\n" +
            "  }, {\n" +
            "    \"keyName\" : \"client_processing\",\n" +
            "    \"project\" : \"DEMO\",\n" +
            "    \"dataStoreKeyName\" : \"processingdb\",\n" +
            "    \"fullTableName\" : \"proc.client\",\n" +
            "    \"sources\" : [ ],\n" +
            "    \"columnSchema\" : [ {\n" +
            "      \"name\" : \"id\",\n" +
            "      \"description\" : \"Client id\",\n" +
            "      \"dataType\" : \"integer\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"name\",\n" +
            "      \"description\" : \"Client name\",\n" +
            "      \"dataType\" : \"varchar(255)\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    }, {\n" +
            "      \"name\" : \"reg_date_time\",\n" +
            "      \"description\" : \"Client registration\",\n" +
            "      \"dataType\" : \"timestamp\",\n" +
            "      \"nullable\" : false,\n" +
            "      \"order\" : null,\n" +
            "      \"sequence\" : false\n" +
            "    } ],\n" +
            "    \"properties\" : {\n" +
            "      \"data-end-point\" : \"proc.client\",\n" +
            "      \"fetchSize\" : \"10000\"\n" +
            "    },\n" +
            "    \"scripts\" : [ {\n" +
            "      \"key\" : \"client_processing.sql\",\n" +
            "      \"order\" : null\n" +
            "    } ],\n" +
            "    \"description\" : \"remote dataset with clients\",\n" +
            "    \"constraints\" : [ {\n" +
            "      \"name\" : \"client_processing_pk\",\n" +
            "      \"type\" : \"primary\",\n" +
            "      \"columns\" : \"id\",\n" +
            "      \"enabled\" : true,\n" +
            "      \"runtimeLevelCheck\" : false,\n" +
            "      \"constructLevelCheck\" : true\n" +
            "    }, {\n" +
            "      \"name\" : \"client_processing_pk\",\n" +
            "      \"type\" : \"unique\",\n" +
            "      \"columns\" : \"name\",\n" +
            "      \"enabled\" : true,\n" +
            "      \"runtimeLevelCheck\" : false,\n" +
            "      \"constructLevelCheck\" : true\n" +
            "    } ]\n" +
            "  } ],\n" +
            "  \"targetDateTime\" : \"2025-01-02T00:00:00Z\",\n" +
            "  \"intervalStartDateTime\" : \"2025-01-01T00:00:00Z\",\n" +
            "  \"intervalEndDateTime\" : \"2025-01-02T00:00:00Z\",\n" +
            "  \"lockSource\" : \"regular.transaction_dds.2025-01-02T00:00:00Z\",\n" +
            "  \"keyBind\" : {\n" +
            "    \"source_DEMO_transaction_dds_keyName\" : \"transaction_dds\",\n" +
            "    \"source_DEMO_client_processing_tableFullName\" : \"proc.client\",\n" +
            "    \"targetIntervalEndTZ\" : \"2025-01-02T00:00:00Z\",\n" +
            "    \"targetIntervalStartTZ\" : \"2025-01-01T00:00:00Z\",\n" +
            "    \"source_DEMO_transaction_processing_keyName\" : \"transaction_processing\",\n" +
            "    \"targetDateTimeTZ\" : \"2025-01-02T00:00:00Z\",\n" +
            "    \"source_DEMO_transaction_dds_tableFullName\" : \"transaction_dds\",\n" +
            "    \"source_DEMO_transaction_processing_tableFullName\" : \"proc.transactions\",\n" +
            "    \"source_DEMO_client_processing_keyName\" : \"client_processing\"\n" +
            "  }\n" +
            "}";
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");

/*
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("lakehouse.task-executor.scheduled.task.kafka.consumer.bootstrap.servers", kafka::getBootstrapServers);

    }*/
    @BeforeAll
    static void beforeAllStart(){
        postgres.start();
    }
    @Test
    void testAddition() throws IOException, TaskFailedException, SQLException {
        SparkSession sparkSession = SparkSession
                .builder()
                .config("spark.driver.extraJavaOptions","--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED")
                .config("spark.executor.extraJavaOptions","--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED")
                .master("local[*]")
               // .master("spark://127.0.0.1:7077")
                //.config("spark.submit.deployMode", "cluster")
                .getOrCreate();

        TaskProcessorConfigDTO conf = ObjectMapping.stringToObject(config, TaskProcessorConfigDTO.class);
        DataStoreDTO pgDataStoreDTO = conf.getDataStores().get("processingdb");
        pgDataStoreDTO.setUrl(postgres.getJdbcUrl());
       // Map<String,String> props = new HashMap<>(pgDataStoreDTO.getProperties());
        pgDataStoreDTO.getProperties().put("password",postgres.getPassword());
        pgDataStoreDTO.getProperties().put("user",postgres.getUsername());
        conf.getDataStores().put("processingdb",pgDataStoreDTO);


        executeJdbcQuery("CREATE SCHEMA proc");
        executeJdbcQuery("CREATE table proc.transactions( " +
                "id  BIGSERIAL,  " +
                "reg_date_time  timestamptz,  " +
                "client_id integer," +
                "provider_id integer, " +
                "amount numeric(10,1), " +
                "commission numeric(10,1))");
        executeJdbcQuery("CREATE table proc.client(" +
                "id integer," +
                "name varchar(255)," +
                "reg_date_time  timestamptz" +
                ")");

        SparkProcessorBodyFactory.buildSparkProcessorBody(sparkSession,conf).run();
        /*org.lakehouse.taskexecutor.executionmodule.body.SparkTaskProcessorBody;
        SparkTaskProcessorBody sparkTaskProcessorBody = new SparkTaskProcessorBody(sparkSession,conf);
        sparkTaskProcessorBody.run();*/
    }
    private void executeJdbcQuery(String sql) throws SQLException {
        String url = postgres.getJdbcUrl();
        String user = postgres.getUsername();
        String password = postgres.getPassword();
        Connection connection = DriverManager.getConnection(url, user, password);
        Statement statement = connection.createStatement();
        statement.execute(sql);
    }

}
