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

package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.processor.body.sql.SQLProcessorBodyAbstract;

public abstract class SparkProcessorBodyAbstract extends SQLProcessorBodyAbstract {
    private final SparkSession sparkSession;
    public SparkProcessorBodyAbstract(
            ConfigRestClientApi configRestClientApi,
            SparkSession sparkSession,
            DataSourceManipulatorFactory dataSourceManipulatorFactory) {
        super(configRestClientApi,dataSourceManipulatorFactory);
        this.sparkSession = sparkSession;
    }

    public SparkSession getSparkSession() {
        return sparkSession;
    }
}
