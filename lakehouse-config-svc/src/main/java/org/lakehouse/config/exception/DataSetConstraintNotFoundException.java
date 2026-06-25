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

package org.lakehouse.config.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class DataSetConstraintNotFoundException extends RuntimeException {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Serial
    private static final long serialVersionUID = 94297551951697828L;
    private static final String msgTemplate = "Constraint with name  %s not found in dataset %s";
    public DataSetConstraintNotFoundException(String constraintName, String datasetName) {

        super(String.format(msgTemplate, constraintName, datasetName));
        logger.error(String.format(msgTemplate, constraintName, datasetName));
    }

}
