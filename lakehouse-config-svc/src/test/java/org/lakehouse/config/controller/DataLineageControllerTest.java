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

package org.lakehouse.config.controller;

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.dataset.DataSetLineageDTO;
import org.lakehouse.config.service.dataset.DataSetLineageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataLineageControllerTest {

    private final DataSetLineageService dataSetLineageService = mock(DataSetLineageService.class);
    private final DataLineageController controller = new DataLineageController(dataSetLineageService);

    @Test
    void getDataSetLineageDTODelegatesToService() {
        DataSetLineageDTO expected = new DataSetLineageDTO();
        when(dataSetLineageService.findLineage("client_processing")).thenReturn(expected);

        DataSetLineageDTO result = controller.getDataSetLineageDTO("client_processing");

        assertThat(result).isSameAs(expected);
        verify(dataSetLineageService).findLineage("client_processing");
    }
}
