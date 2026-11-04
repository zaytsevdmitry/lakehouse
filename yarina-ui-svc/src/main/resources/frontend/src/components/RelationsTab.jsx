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
import { fetchDataSet } from '../api.js';

const COLUMN_WIDTH = 320;
const ROW_GAP = 60;

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

function toNode(dataSet, position) {
  return {
    id: `node:${dataSet.keyName}`,
    type: 'erNode',
    position,
    data: {
      label: dataSet.keyName,
      composite: compositeName(dataSet),
      columns: dataSet.columnSchema || [],
    },
  };
}

/**
 * Lays the relations out left-to-right. The selected data set is the source of
 * every FOREIGN reference, so the referenced (outgoing) data sets are stacked in
 * a column on the right; the layout is not clamped to the viewport, so a long
 * list simply extends beyond the edges and can be panned/dragged.
 */
export function buildRelationsLayout(center, neighbors) {
  const nodes = [toNode(center, { x: 0, y: 0 })];
  const sorted = [...(neighbors || [])].sort((a, b) => String(a.keyName).localeCompare(String(b.keyName)));
  sorted.forEach((neighbor, i) => {
    nodes.push(toNode(neighbor, {
      x: COLUMN_WIDTH,
      y: (i - (sorted.length - 1) / 2) * ROW_GAP,
    }));
  });
  return nodes;
}

export default function RelationsTab({ dataSet }) {
  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [expanded, setExpanded] = useState(false);

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

        const resultNodes = buildRelationsLayout(dataSet, [...neighborByKey.values()]);

        const resultEdges = foreignRefs
          .filter((ref) => neighborByKey.has(ref.dataSetKeyName))
          .map((ref) => ({
            id: `edge:${dataSet.keyName}:${ref.dataSetKeyName}:${ref.name}`,
            source: `node:${dataSet.keyName}`,
            target: `node:${ref.dataSetKeyName}`,
            label: ref.name,
            sourceHandle: 'source-right',
            targetHandle: 'target-left',
            markerEnd: { type: MarkerType.ArrowClosed },
          }));

        setNodes(resultNodes);
        setEdges(resultEdges);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [dataSet]);

  // Relations are movable: drag/selection changes are applied to the local nodes.
  const onNodesChange = useCallback((changes) => {
    setNodes((nds) => applyNodeChanges(changes, nds));
  }, []);

  if (!dataSet) {
    return <div className="empty-box">Select a table in the tree.</div>;
  }

  return (
    <div className="relations-tab">
      {error && <div className="error-box">Error: {error}</div>}
      {loading && <div className="empty-box">Loading...</div>}
      {!error && !loading && (
        <div className={`relations-host${expanded ? ' diagram-fullscreen' : ''}`}>
          <ReactFlow
            key={dataSet.keyName}
            className="relations-flow"
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
