import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ReactFlow,
  Background,
  BackgroundVariant,
  Controls,
  MarkerType,
  Handle,
  Position,
  applyNodeChanges,
  applyEdgeChanges,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

const positions = new Map();

const nodeKey = (a) => (a && (a.keyName != null ? a.keyName : a.name));

/**
 * Flow editor for a "dag" field. Nodes come from a sibling list field
 * (`nodeField`), edges from the dag field itself (list of {from, to}).
 * The whole document is the source of truth; edits rebuild it via onDocChange.
 */
export default function DagEditor({ doc, nodeField, edgeField, onDocChange, readOnly }) {
  const acts = useMemo(
    () => (Array.isArray(doc && doc[nodeField]) ? doc[nodeField] : []),
    [doc, nodeField],
  );
  const edgesMeta = useMemo(
    () => (Array.isArray(doc && doc[edgeField]) ? doc[edgeField] : []),
    [doc, edgeField],
  );

  const nodes = useMemo(() => {
    const incoming = new Map();
    for (const e of edgesMeta) incoming.set(e.to, (incoming.get(e.to) || 0) + 1);
    const byCol = new Map();
    const indexInCol = new Map();
    for (const act of acts) {
      const id = nodeKey(act);
      const col = acts.some((a) => nodeKey(a) === id) && incoming.get(id) ? 1 : 0;
      const key = String(col);
      byCol.set(key, (byCol.get(key) || 0) + 1);
      indexInCol.set(id, byCol.get(key));
    }
    return acts.map((act, i) => {
      const id = String(nodeKey(act) != null ? nodeKey(act) : `node-${i}`);
      const col = incoming.has(id) ? 1 : 0;
      let position = positions.get(id);
      if (!position) {
        const count = byCol.get(String(col)) || 1;
        position = { x: 40 + col * 280, y: 40 + ((indexInCol.get(id) || 1) - 1) * 150 - ((count - 1) * 150) / 2 };
        positions.set(id, position);
      }
      return {
        id,
        type: 'configNode',
        position,
        data: { keyName: nodeKey(act), description: act.description || '', readOnly, nodeIndex: i },
      };
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [acts, edgeField, readOnly]);

  const edges = useMemo(
    () => edgesMeta
      .filter((e) => e && e.from && e.to)
      .map((e) => ({
        id: `${e.from}->${e.to}`,
        source: String(e.from),
        target: String(e.to),
        markerEnd: { type: MarkerType.ArrowClosed },
        style: { stroke: '#2563eb', strokeWidth: 1.5 },
      })),
    [edgesMeta],
  );

  const [flowNodes, setFlowNodes] = useState(nodes);
  const [flowEdges, setFlowEdges] = useState(edges);

  const actsKey = JSON.stringify(acts);
  const edgesKey = JSON.stringify(edgesMeta);

  useEffect(() => {
    setFlowNodes(nodes);
  }, [actsKey, nodeField]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    setFlowEdges(edges);
  }, [edgesKey, edgeField]); // eslint-disable-line react-hooks/exhaustive-deps

  const syncNodes = (nextNodes) => {
    setFlowNodes(nextNodes);
    const removed = nextNodes.filter((n) => !flowNodes.find((o) => o.id === n.id));
    if (removed.length > 0) {
      const ids = new Set(removed.map((n) => n.id));
      onDocChange({
        ...doc,
        [nodeField]: (doc[nodeField] || []).filter((a) => !ids.has(String(nodeKey(a)))),
        [edgeField]: (doc[edgeField] || []).filter((e) => !ids.has(String(e.from)) && !ids.has(String(e.to))),
      });
    }
  };

  const syncEdges = (nextEdges) => {
    setFlowEdges(nextEdges);
    const removed = nextEdges.filter((e) => !flowEdges.find((o) => o.id === e.id));
    if (removed.length > 0) {
      const dead = new Set(removed.map((e) => e.id));
      const list = (doc[edgeField] || []).filter((meta) => {
        const id = `${meta.from}->${meta.to}`;
        return !dead.has(id);
      });
      onDocChange({ ...doc, [edgeField]: list });
    }
  };

  const onConnect = useCallback((connection) => {
    const meta = { from: connection.source, to: connection.target };
    onDocChange({
      ...doc,
      [edgeField]: [...(doc[edgeField] || []), meta],
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [doc, edgeField, onDocChange]);

  const addAct = () => {
    const list = doc[nodeField] || [];
    const idKey = list.some((a) => Object.prototype.hasOwnProperty.call(a, 'keyName')) ? 'keyName' : 'name';
    const base = `act_${list.length + 1}`;
    let name = base;
    let n = 2;
    while (list.some((a) => nodeKey(a) === name)) {
      name = `${base}_${n}`;
      n += 1;
    }
    onDocChange({
      ...doc,
      [nodeField]: [...list, { [idKey]: name, enabled: true, scenarioActTemplate: null }],
    });
  };

  const renameAct = (oldName, newName) => {
    const clean = newName.trim();
    if (!clean || clean === oldName) return;
    const list = doc[nodeField] || [];
    const idKey = list.some((a) => Object.prototype.hasOwnProperty.call(a, 'keyName')) ? 'keyName' : 'name';
    const nextList = list.map((a) => (nodeKey(a) === oldName ? { ...a, [idKey]: clean } : a));
    const edgesList = (doc[edgeField] || []).map((e) => ({
      from: e.from === oldName ? clean : e.from,
      to: e.to === oldName ? clean : e.to,
    }));
    positions.set(clean, positions.get(oldName));
    positions.delete(oldName);
    onDocChange({ ...doc, [nodeField]: nextList, [edgeField]: edgesList });
  };

  const nodeTypes = useMemo(() => ({
    configNode: (props) => (
      <ConfigNode
        {...props}
        readOnly={readOnly}
        onRename={renameAct}
        onRemove={(id) => {
          const ids = new Set([id]);
          onDocChange({
            ...doc,
            [nodeField]: (doc[nodeField] || []).filter((a) => !ids.has(String(nodeKey(a)))),
            [edgeField]: (doc[edgeField] || []).filter((e) => e.from !== id && e.to !== id),
          });
          positions.delete(id);
        }}
      />
    ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }), [doc, nodeField, edgeField, readOnly, onDocChange]);

  return (
    <div className="dag-host">
      <ReactFlow
        nodes={flowNodes}
        edges={flowEdges}
        onNodesChange={(changes) => {
          if (!readOnly) syncNodes(applyNodeChanges(changes, flowNodes));
        }}
        onEdgesChange={(changes) => {
          if (!readOnly) syncEdges(applyEdgeChanges(changes, flowEdges));
        }}
        onConnect={readOnly ? undefined : onConnect}
        nodeTypes={nodeTypes}
        fitView
        proOptions={{ hideAttribution: true }}
        minZoom={0.3}
      >
        <Background variant={BackgroundVariant.Dots} gap={18} size={1} />
        <Controls />
      </ReactFlow>
      {!readOnly && (
        <div className="dag-toolbar">
          <button type="button" onClick={addAct}>+ Add step</button>
          <span className="muted small">Drag from the right handle to connect steps. Select an edge to delete it.</span>
        </div>
      )}
    </div>
  );
}

function ConfigNode({ id, data, selected, onRename, onRemove }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(data.keyName);
  return (
    <div className={`cfg-node${selected ? ' selected' : ''}`}>
      <Handle type="target" position={Position.Left} />
      {editing ? (
        <input
          value={draft}
          autoFocus
          onChange={(e) => setDraft(e.target.value)}
          onBlur={() => { onRename(data.keyName, draft); setEditing(false); }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') { onRename(data.keyName, draft); setEditing(false); }
            if (e.key === 'Escape') { setDraft(data.keyName); setEditing(false); }
          }}
        />
      ) : (
        <div className="cfg-node-title" onDoubleClick={() => !data.readOnly && setEditing(true)}>
          <strong>{data.keyName}</strong>
        </div>
      )}
      {data.description && <div className="cfg-node-desc">{data.description}</div>}
      {!data.readOnly && (
        <button className="danger small cfg-node-remove" onClick={() => onRemove(id)} title="Remove step">×</button>
      )}
      <Handle type="source" position={Position.Right} />
    </div>
  );
}