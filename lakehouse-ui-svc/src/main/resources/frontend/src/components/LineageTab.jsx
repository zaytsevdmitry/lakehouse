import React, { useEffect, useState } from 'react';
import { Background, Controls, MarkerType, ReactFlow } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { fetchLineage } from '../api.js';

const CENTER_X = 400;
const CENTER_Y = 200;
const COLUMN_WIDTH = 260;
const VERTICAL_GAP = 90;
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

function buildLayout(center, vertices, edges) {
  const downstream = new Map();
  const upstream = new Map();
  edges.forEach((e) => {
    if (!downstream.has(e.from)) downstream.set(e.from, []);
    downstream.get(e.from).push(e.to);
    if (!upstream.has(e.to)) upstream.set(e.to, []);
    upstream.get(e.to).push(e.from);
  });

  const downDist = new Map([[center, 0]]);
  const upDist = new Map([[center, 0]]);

  let queue = [center];
  let qi = 0;
  while (qi < queue.length) {
    const node = queue[qi++];
    (downstream.get(node) || []).forEach((target) => {
      if (!downDist.has(target)) {
        downDist.set(target, downDist.get(node) + 1);
        queue.push(target);
      }
    });
  }
  queue = [center];
  qi = 0;
  while (qi < queue.length) {
    const node = queue[qi++];
    (upstream.get(node) || []).forEach((source) => {
      if (!upDist.has(source)) {
        upDist.set(source, upDist.get(node) + 1);
        queue.push(source);
      }
    });
  }

  const upstreamKeys = vertices.filter((v) => upDist.has(v) && !downDist.has(v));
  const downstreamKeys = vertices.filter((v) => downDist.has(v) && !upDist.has(v));

  const nodes = [
    {
      id: `node:${center}`,
      position: { x: CENTER_X, y: CENTER_Y },
      data: { label: center },
      style: CENTER_STYLE,
    },
  ];

  const placeLayer = (keys, distMap, direction) => {
    const layers = new Map();
    keys.forEach((k) => {
      const d = distMap.get(k);
      if (!layers.has(d)) layers.set(d, []);
      layers.get(d).push(k);
    });
    [...layers.keys()].sort((a, b) => a - b).forEach((d) => {
      const layerKeys = layers.get(d);
      layerKeys.forEach((key, i) => {
        nodes.push({
          id: `node:${key}`,
          position: {
            x: CENTER_X + direction * d * COLUMN_WIDTH,
            y: CENTER_Y + (i - (layerKeys.length - 1) / 2) * VERTICAL_GAP,
          },
          data: { label: key },
          style: SIDE_STYLE,
        });
      });
    });
  };

  placeLayer(upstreamKeys, upDist, -1);
  placeLayer(downstreamKeys, downDist, 1);

  const nodeIds = new Set(nodes.map((n) => n.id));
  const flowEdges = edges
    .filter((e) => nodeIds.has(`node:${e.from}`) && nodeIds.has(`node:${e.to}`))
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
        const layout = buildLayout(dataSetKeyName, lineage.vertices || [], lineage.edges || []);
        setNodes(layout.nodes);
        setEdges(layout.edges);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [dataSetKeyName]);

  if (!dataSetKeyName) {
    return <div className="empty-box">Select a table in the tree.</div>;
  }

  return (
    <div className="lineage-tab">
      {error && <div className="error-box">Error: {error}</div>}
      {loading && <div className="empty-box">Loading...</div>}
      {!error && !loading && (
        <div className="lineage-flow">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            fitView
            fitViewOptions={{ padding: 0.2 }}
            proOptions={{ hideAttribution: true }}
          >
            <Background />
            <Controls />
          </ReactFlow>
        </div>
      )}
    </div>
  );
}

export default LineageTab;
