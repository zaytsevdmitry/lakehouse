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

import static guru.nidi.graphviz.model.Factory.*;


public class GraphBuilder {
    public static void build2(ScheduleEffectiveDTO dto){
        MutableGraph root = mutGraph(dto.getName()).setDirected(true).graphAttrs().add("compound", true);

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