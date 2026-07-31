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
package org.lakehouse.task.executor.spark.api.configuration;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Properties;

@Configuration
public class SparkSessionConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SparkSessionConfiguration.class);

    private static final String DEFAULT_REDACTION_REGEX = "(?i)secret|password|token|access[.]key|credentials|private";

    @Bean
    SparkSession getSparkSession() {
        SparkSession.Builder builder = SparkSession.builder();
        Properties sysProps = System.getProperties();

        int count = 0;
        boolean hasUserRedaction = false;
        boolean hasUserSqlRedaction = false;

        for (String key : sysProps.stringPropertyNames()) {
            if (key.startsWith("spark.")) {
                String value = sysProps.getProperty(key);
                builder.config(key, value);
                count++;

                if ("spark.redaction.regex".equals(key)) {
                    hasUserRedaction = true;
                }
                if ("spark.sql.redaction.string.regex".equals(key)) {
                    hasUserSqlRedaction = true;
                }
            }
        }

        if (!hasUserRedaction) {
            builder.config("spark.redaction.regex", DEFAULT_REDACTION_REGEX);
            log.debug("Property 'spark.redaction.regex' was not provided. Using secure default.");
        }
        if (!hasUserSqlRedaction) {
            builder.config("spark.sql.redaction.string.regex", DEFAULT_REDACTION_REGEX);
            log.debug("Property 'spark.sql.redaction.string.regex' was not provided. Using secure default.");
        }

        log.info("SparkSession created with {} spark.* properties from system context", count);
        return builder.getOrCreate();
    }
}
