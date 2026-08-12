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

package org.lakehouse.config.service.dataset;

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetLineageDTO;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dataset.DataSetSource;
import org.lakehouse.config.repository.dataset.DataSetSourceRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSetLineageServiceTest {

    private final DataSetSourceRepository dataSetSourceRepository = mock(DataSetSourceRepository.class);
    private final DataSetLineageService dataSetLineageService =
            new DataSetLineageService(dataSetSourceRepository);

    private DataSet dataSet(String keyName) {
        DataSet dataSet = new DataSet();
        dataSet.setKeyName(keyName);
        return dataSet;
    }

    private DataSetSource dataSetSource(String sourceKeyName, String dataSetKeyName) {
        DataSetSource dataSetSource = new DataSetSource();
        if (sourceKeyName != null) {
            dataSetSource.setSource(dataSet(sourceKeyName));
        }
        if (dataSetKeyName != null) {
            dataSetSource.setDataSet(dataSet(dataSetKeyName));
        }
        return dataSetSource;
    }

    @Test
    void findLineageBuildsVerticesAndEdgesBothDirections() {
        DataSetSource upstream = dataSetSource("source_a", "target_ds");
        DataSetSource downstream = dataSetSource("target_ds", "consumer_b");

        when(dataSetSourceRepository.findBySourceKeyName("target_ds")).thenReturn(List.of(downstream));
        when(dataSetSourceRepository.findByDataSetKeyName("target_ds")).thenReturn(List.of(upstream));

        DataSetLineageDTO lineage = dataSetLineageService.findLineage("target_ds");

        assertThat(lineage.getVertices()).containsExactly("target_ds", "consumer_b", "source_a");
        assertThat(lineage.getEdges()).containsExactlyInAnyOrder(
                dagEdge("target_ds", "consumer_b"),
                dagEdge("source_a", "target_ds"));

        verify(dataSetSourceRepository).findBySourceKeyName("target_ds");
        verify(dataSetSourceRepository).findBySourceKeyName("consumer_b");
        verify(dataSetSourceRepository).findByDataSetKeyName("target_ds");
        verify(dataSetSourceRepository).findByDataSetKeyName("source_a");
    }

    @Test
    void findLineageTraversesGraphBreadthFirst() {
        when(dataSetSourceRepository.findBySourceKeyName("target_ds"))
                .thenReturn(List.of(dataSetSource("target_ds", "consumer_b")));
        when(dataSetSourceRepository.findByDataSetKeyName("target_ds"))
                .thenReturn(List.of(dataSetSource("source_a", "target_ds")));
        when(dataSetSourceRepository.findBySourceKeyName("consumer_b"))
                .thenReturn(List.of(dataSetSource("consumer_b", "leaf_c")));
        when(dataSetSourceRepository.findByDataSetKeyName("source_a"))
                .thenReturn(List.of(dataSetSource("grand_source", "source_a")));

        DataSetLineageDTO lineage = dataSetLineageService.findLineage("target_ds");

        assertThat(lineage.getVertices()).containsExactly(
                "target_ds", "consumer_b", "source_a", "leaf_c", "grand_source");
        assertThat(lineage.getEdges()).containsExactlyInAnyOrder(
                dagEdge("target_ds", "consumer_b"),
                dagEdge("source_a", "target_ds"),
                dagEdge("consumer_b", "leaf_c"),
                dagEdge("grand_source", "source_a"));
    }

    @Test
    void findLineageSkipsRowsWithoutDataSet() {
        DataSetSource qualityOnly = dataSetSource("source_a", null);
        when(dataSetSourceRepository.findBySourceKeyName("source_a")).thenReturn(List.of(qualityOnly));
        when(dataSetSourceRepository.findByDataSetKeyName("source_a")).thenReturn(List.of());

        DataSetLineageDTO lineage = dataSetLineageService.findLineage("source_a");

        assertThat(lineage.getVertices()).containsExactly("source_a");
        assertThat(lineage.getEdges()).isEmpty();
    }

    @Test
    void findLineageReturnsOnlySelfVertexWhenNoRelations() {
        when(dataSetSourceRepository.findBySourceKeyName("lonely_ds")).thenReturn(List.of());
        when(dataSetSourceRepository.findByDataSetKeyName("lonely_ds")).thenReturn(List.of());

        DataSetLineageDTO lineage = dataSetLineageService.findLineage("lonely_ds");

        assertThat(lineage.getVertices()).containsExactly("lonely_ds");
        assertThat(lineage.getEdges()).isEmpty();
    }

    private DagEdgeDTO dagEdge(String from, String to) {
        DagEdgeDTO edge = new DagEdgeDTO();
        edge.setFrom(from);
        edge.setTo(to);
        return edge;
    }
}
