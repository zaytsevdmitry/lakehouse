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

public class TaskProcessorArgKey {
    public static String QUALITY_METRICS_CONF_KEY_NAME ="qualityMetricsConfKeyName";
    public static String SPARK_PREFIX = "spark.";
    public static String SPARK_CATALOG_PREFIX = "spark.sql.catalog.";
    public static String K8S_SPARK_OPERATOR = "k8s.spark-operator.";
    public static String K8S_SPARK_OPERATOR_MANIFEST = "manifest.";

    public static String K8S_NATIVE = "k8s.spark-native.";
    public static String K8S_NATIVE_MANIFEST = "manifest.";

}
