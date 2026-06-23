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

package org.lakehouse.config;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleScenarioActEffectiveDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;


public class GraphBuilder {
    public static void build2(ScheduleEffectiveDTO dto){
        MutableGraph root = mutGraph(dto.getKeyName()).setDirected(true).graphAttrs().add("compound", true);

        Map<String,MutableGraph> clusters = new HashMap<>();
        for (ScheduleScenarioActEffectiveDTO efa:dto.getScenarioActs()){
            MutableGraph sub = mutGraph(efa.getName()).setDirected(true).setCluster(true).add(mutNode(efa.getName()));
            for (DagEdgeDTO edgeDTO :efa.getDagEdges()){
                sub.add(
                        mutNode(efa.getName() + "." + edgeDTO.getFrom()).addLink(efa.getName() + "." + edgeDTO.getTo())
                );
            }
            //sub.addTo(root);
            root.add(sub);
            clusters.put(efa.getName(),sub);
        }
        for (DagEdgeDTO entry : dto.getScenarioActEdges()) {
            root.add(
                    mutNode(entry.getFrom()).addLink(mutNode(entry.getTo()))
            );
        }

        String svgOutput = Graphviz.fromGraph(root)
                .width(400)
                .render(Format.SVG)
                .toString();
        System.out.println("\n--- Код SVG (фрагмент) ---");
        System.out.println(svgOutput.substring(0, 100) + "...");

    }
    public static void build(Set<DagEdgeDTO> edges) {
        // 1. Описываем структуру графа программно
        MutableGraph root = mutGraph("example_graph").setDirected(true);
        for (DagEdgeDTO entry : edges) {
            root.add(
                    mutNode(entry.getFrom()).addLink(mutNode(entry.getTo()))
            );
        }

        // 2. Получаем исходный код на языке DOT в виде строки
        String dotCode = Graphviz.fromGraph(root)
                .render(Format.DOT)
                .toString();

        System.out.println("--- Код DOT ---");
        System.out.println(dotCode);

        // 3. Получаем визуализацию в формате SVG (тоже в виде строки)
        // Это удобно для вставки напрямую в HTML
        String svgOutput = Graphviz.fromGraph(root)
                .width(400)
                .render(Format.SVG)
                .toString();

        System.out.println("\n--- Код SVG (фрагмент) ---");
        System.out.println(svgOutput.substring(0, 100) + "...");
    }
}