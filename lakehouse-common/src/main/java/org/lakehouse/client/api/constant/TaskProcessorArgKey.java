/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
