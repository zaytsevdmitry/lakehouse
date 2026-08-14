import React, { useEffect, useState } from 'react';
import { Background, Controls, Handle, MarkerType, Position, ReactFlow } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { fetchDataSet } from '../api.js';

const NODE_W = 240;
const NODE_H = 200;
const CENTER_X = 0;
const CENTER_Y = 0;
const RADIUS = 360;

function compositeName(dataSet) {
  return [dataSet.dataSourceKeyName, dataSet.databaseSchemaName, dataSet.tableName]
    .filter(Boolean)
    .join('.');
}

const HANDLE_SIDES = [
  { id: 'top', position: Position.Top },
  { id: 'right', position: Position.Right },
  { id: 'bottom', position: Position.Bottom },
  { id: 'left', position: Position.Left },
];

function EntityNode({ data }) {
  return (
    <div className="er-node">
      {HANDLE_SIDES.map((side) => (
        <Handle
          key={`target-${side.id}`}
          id={`target-${side.id}`}
          type="target"
          position={side.position}
          className={`er-handle er-handle--${side.id}`}
        />
      ))}
      {HANDLE_SIDES.map((side) => (
        <Handle
          key={`source-${side.id}`}
          id={`source-${side.id}`}
          type="source"
          position={side.position}
          className={`er-handle er-handle--${side.id}`}
        />
      ))}
      <div className="er-node-title">{data.label}</div>
      <div className="er-node-subtitle">{data.composite}</div>
      <div className="er-node-columns">
        {(data.columns || []).map((column, idx) => (
          <div className="er-node-column" key={idx}>
            <span className="er-node-column-name">{column.name}</span>
            <span className="er-node-column-type">{column.dataType}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

const NODE_TYPES = { erNode: EntityNode };

function connectionSides(sourceNode, targetNode) {
  const sourceCenter = {
    x: sourceNode.position.x + NODE_W / 2,
    y: sourceNode.position.y + NODE_H / 2,
  };
  const targetCenter = {
    x: targetNode.position.x + NODE_W / 2,
    y: targetNode.position.y + NODE_H / 2,
  };
  const dx = targetCenter.x - sourceCenter.x;
  const dy = targetCenter.y - sourceCenter.y;
  if (Math.abs(dx) >= Math.abs(dy)) {
    const horizontal = dx >= 0 ? 'right' : 'left';
    return { source: horizontal, target: horizontal === 'right' ? 'left' : 'right' };
  }
  const vertical = dy >= 0 ? 'bottom' : 'top';
  return { source: vertical, target: vertical === 'bottom' ? 'top' : 'bottom' };
}

function toNode(dataSet) {
  return {
    id: `node:${dataSet.keyName}`,
    type: 'erNode',
    position: { x: CENTER_X - NODE_W / 2, y: CENTER_Y - NODE_H / 2 },
    data: {
      label: dataSet.keyName,
      composite: compositeName(dataSet),
      columns: dataSet.columnSchema || [],
    },
  };
}

export default function RelationsTab({ dataSet }) {
  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!dataSet) {
      setNodes([]);
      setEdges([]);
      return;
    }
    setLoading(true);
    setError('');

    const constraints = dataSet.constraints || {};
    const foreignRefs = Object.entries(constraints)
      .filter(([, constraint]) => constraint && constraint.type === 'foreign')
      .map(([name, constraint]) => ({
        name,
        dataSetKeyName: constraint.reference ? constraint.reference.dataSetKeyName : null,
      }))
      .filter((ref) => ref.dataSetKeyName);

    const uniqueKeys = [...new Set(foreignRefs.map((ref) => ref.dataSetKeyName))];

    Promise.all(uniqueKeys.map((key) => fetchDataSet(key).catch(() => null)))
      .then((neighbors) => {
        const neighborByKey = new Map();
        uniqueKeys.forEach((key, i) => {
          if (neighbors[i]) neighborByKey.set(key, neighbors[i]);
        });

        const neighborNodes = [...neighborByKey.values()];
        const centerNode = toNode(dataSet);

        const resultNodes = [
          centerNode,
          ...neighborNodes.map((neighbor, i) => {
            const angle = neighborNodes.length === 1 ? -Math.PI / 2 : (i / neighborNodes.length) * 2 * Math.PI;
            return {
              ...toNode(neighbor),
              position: {
                x: CENTER_X + RADIUS * Math.cos(angle) - NODE_W / 2,
                y: CENTER_Y + RADIUS * Math.sin(angle) - NODE_H / 2,
              },
            };
          }),
        ];

        const resultEdges = foreignRefs
          .filter((ref) => neighborByKey.has(ref.dataSetKeyName))
          .map((ref) => {
            const sourceNode = resultNodes.find((n) => n.id === `node:${dataSet.keyName}`);
            const targetNode = resultNodes.find((n) => n.id === `node:${ref.dataSetKeyName}`);
            const sides = sourceNode && targetNode ? connectionSides(sourceNode, targetNode) : null;
            return {
              id: `edge:${dataSet.keyName}:${ref.dataSetKeyName}:${ref.name}`,
              source: `node:${dataSet.keyName}`,
              target: `node:${ref.dataSetKeyName}`,
              label: ref.name,
              sourceHandle: sides ? `source-${sides.source}` : undefined,
              targetHandle: sides ? `target-${sides.target}` : undefined,
              markerEnd: { type: MarkerType.ArrowClosed },
            };
          });

        setNodes(resultNodes);
        setEdges(resultEdges);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [dataSet]);

  if (!dataSet) {
    return <div className="empty-box">Select a table in the tree.</div>;
  }

  return (
    <div className="relations-tab">
      {error && <div className="error-box">Error: {error}</div>}
      {loading && <div className="empty-box">Loading...</div>}
      {!error && !loading && (
        <div className="relations-flow">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={NODE_TYPES}
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
