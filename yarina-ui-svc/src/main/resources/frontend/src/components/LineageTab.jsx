import React, { useCallback, useEffect, useState } from 'react';
import {
  applyNodeChanges,
  Background,
  BackgroundVariant,
  Controls,
  Handle,
  MarkerType,
  Position,
  ReactFlow,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { fetchLineage } from '../api.js';

function LineageNode({ data }) {
  return (
    <div className="lineage-node">
      <Handle type="target" position={Position.Left} className="dag-handle dag-handle--target" />
      <Handle type="source" position={Position.Right} className="dag-handle dag-handle--source" />
      {data.label}
    </div>
  );
}

const NODE_TYPES = { lineageNode: LineageNode };

const COLUMN_WIDTH = 290;
const ROW_GAP = 80;
const CENTER_STYLE = {
  background: 'var(--accent, #2f6fed)',
  color: '#ffffff',
  border: '1px solid #1a4bb8',
  borderRadius: '6px',
  padding: '10px 14px',
  fontWeight: 600,
};
const SIDE_STYLE = {
  background: 'var(--panel, #ffffff)',
  border: '1px solid var(--border, #ccc)',
  borderRadius: '6px',
  padding: '10px 14px',
};

/**
 * Breadth-first distances from {@code start} over the given adjacency map.
 * The center itself is distance 0; unreachable vertices are absent.
 */
function distances(start, adjacency) {
  const dist = new Map([[start, 0]]);
  const queue = [start];
  let qi = 0;
  while (qi < queue.length) {
    const node = queue[qi++];
    (adjacency.get(node) || []).forEach((next) => {
      if (!dist.has(next)) {
        dist.set(next, dist.get(node) + 1);
        queue.push(next);
      }
    });
  }
  return dist;
}

/**
 * Lays the lineage out left-to-right: everything that feeds the selected data set
 * (incoming/upstream) goes into columns on the left, everything it feeds
 * (outgoing/downstream) goes into columns on the right. Vertices that are both
 * (cycles) are placed on the side they are closest to. The layout is not clamped
 * to the viewport, so diagrams that do not fit simply extend beyond the edges and
 * can be panned/dragged.
 */
export function buildLineageLayout(center, vertices, edges) {
  const forward = new Map();
  const backward = new Map();
  const add = (map, key, value) => {
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(value);
  };
  (edges || []).forEach((e) => {
    if (!e || !e.from || !e.to) return;
    add(forward, e.from, e.to);
    add(backward, e.to, e.from);
  });

  const fromCenter = distances(center, forward);
  const toCenter = distances(center, backward);

  const layers = new Map();
  const place = (key, side, depth) => {
    const layerKey = `${side}:${depth}`;
    if (!layers.has(layerKey)) layers.set(layerKey, []);
    layers.get(layerKey).push(key);
  };

  const allVertices = [...new Set([...(vertices || []), center])].filter(Boolean);
  allVertices.forEach((key) => {
    if (key === center) return;
    const out = fromCenter.get(key);
    const inc = toCenter.get(key);
    if (inc != null && (out == null || inc <= out)) {
      place(key, 'left', inc);
    } else if (out != null) {
      place(key, 'right', out);
    } else {
      // Vertices of the response that are not connected to the selected data set
      // still belong on the canvas; keep them out of the center column.
      place(key, 'right', 1);
    }
  });

  const nodes = [
    {
      id: `node:${center}`,
      type: 'lineageNode',
      position: { x: 0, y: 0 },
      data: { label: center },
      style: CENTER_STYLE,
    },
  ];

  layers.forEach((keys, layerKey) => {
    const [side, depthValue] = layerKey.split(':');
    const depth = Number(depthValue);
    const sorted = [...keys].sort((a, b) => String(a).localeCompare(String(b)));
    const direction = side === 'left' ? -1 : 1;
    sorted.forEach((key, i) => {
      nodes.push({
        id: `node:${key}`,
        type: 'lineageNode',
        position: {
          x: direction * depth * COLUMN_WIDTH,
          y: (i - (sorted.length - 1) / 2) * ROW_GAP,
        },
        data: { label: key },
        style: SIDE_STYLE,
      });
    });
  });

  const nodeIds = new Set(nodes.map((n) => n.id));
  const flowEdges = (edges || [])
    .filter((e) => e && nodeIds.has(`node:${e.from}`) && nodeIds.has(`node:${e.to}`))
    .map((e) => ({
      id: `edge:${e.from}:${e.to}`,
      source: `node:${e.from}`,
      target: `node:${e.to}`,
      markerEnd: { type: MarkerType.ArrowClosed },
    }));

  return { nodes, edges: flowEdges };
}

function LineageTab({ dataSetKeyName }) {
  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    if (!dataSetKeyName) {
      setNodes([]);
      setEdges([]);
      return;
    }
    setLoading(true);
    setError('');
    fetchLineage(dataSetKeyName)
      .then((lineage) => {
        const layout = buildLineageLayout(dataSetKeyName, lineage.vertices || [], lineage.edges || []);
        setNodes(layout.nodes);
        setEdges(layout.edges);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [dataSetKeyName]);

  // Nodes are movable: every drag/selection change is applied to the local node
  // array, so the user can rearrange the automatic layout.
  const onNodesChange = useCallback((changes) => {
    setNodes((nds) => applyNodeChanges(changes, nds));
  }, []);

  if (!dataSetKeyName) {
    return <div className="empty-box">Select a table in the tree.</div>;
  }

  return (
    <div className="lineage-tab">
      {error && <div className="error-box">Error: {error}</div>}
      {loading && <div className="empty-box">Loading...</div>}
      {!error && !loading && (
        <div className={`lineage-host${expanded ? ' diagram-fullscreen' : ''}`}>
          <ReactFlow
            key={dataSetKeyName}
            className="lineage-flow"
            nodes={nodes}
            edges={edges}
            nodeTypes={NODE_TYPES}
            onNodesChange={onNodesChange}
            fitView
            fitViewOptions={{ padding: 0.2, minZoom: 0.8, maxZoom: 1 }}
            minZoom={0.1}
            nodesConnectable={false}
            elementsSelectable
            deleteKeyCode={null}
            proOptions={{ hideAttribution: true }}
          >
            <Background variant={BackgroundVariant.Dots} gap={18} size={1.2} />
            <Controls />
          </ReactFlow>
          <button
            type="button"
            className={`diagram-expand${expanded ? ' diagram-expand--on' : ''}`}
            title={expanded ? 'Restore diagram size' : 'Expand diagram to full screen'}
            onClick={() => setExpanded((v) => !v)}
          >{expanded ? '>-<' : '<-->'}</button>
        </div>
      )}
    </div>
  );
}

export default LineageTab;
