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

public class KeyValueEntityMergeException extends RuntimeException {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String msgTemplate = "Merge property error with name  %s  %s";
    public KeyValueEntityMergeException(String propertyName, String errmsg, Throwable cause) {

        super(String.format(msgTemplate, propertyName, errmsg),cause);
        logger.error(String.format(msgTemplate, propertyName, errmsg));
    }

}
