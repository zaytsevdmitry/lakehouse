import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Background,
  Controls,
  Handle,
  MarkerType,
  Position,
  ReactFlow,
  applyNodeChanges,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { fetchServiceEdges, fetchServiceVertices } from '../api.js';

const NODE_W = 220;
const NODE_H = 80;
const H_GAP = 80;
const V_GAP = 40;
const H_STEP = NODE_W + H_GAP;
const V_STEP = NODE_H + V_GAP;

const NODE_TYPES = {
  serviceNode: ServiceNode,
};

const isBackLink = (from, to) =>
  from.startsWith('lakehouse-task-executor') && to === 'lakehouse-scheduler-svc';

function ServiceNode({ data }) {
  const status = data.status || 'DOWN';
  return (
    <div className={`service-node service-node--${status.toLowerCase()}`}>
      <Handle
        type="target"
        id="target-left"
        position={Position.Left}
        style={{ top: '50%' }}
      />
      <Handle
        type="source"
        id="source-right"
        position={Position.Right}
        style={{ top: '50%' }}
      />
      <div className="service-node-name">{data.key}</div>
      <div className="service-node-label">{data.label}</div>
      <div className="service-node-status">{status}</div>
    </div>
  );
}

export default function ServicesSection({ services, error }) {
  const [edgesConfig, setEdgesConfig] = useState({});
  const [edgesError, setEdgesError] = useState('');
  const [verticesConfig, setVerticesConfig] = useState({});
  const [verticesError, setVerticesError] = useState('');

  useEffect(() => {
    fetchServiceEdges()
      .then(setEdgesConfig)
      .catch((e) => setEdgesError(e.message));
    fetchServiceVertices()
      .then(setVerticesConfig)
      .catch((e) => setVerticesError(e.message));
  }, []);

  const serviceStatusMap = useMemo(() => {
    const map = new Map();
    services.forEach((s) => map.set(s.name, s.status || 'DOWN'));
    return map;
  }, [services]);

  const { layoutNodes, edges } = useMemo(() => {
    const vertexEntries = Object.entries(verticesConfig);
    const names = vertexEntries.map(([key]) => key);
    const nameSet = new Set(names);

    const outgoing = new Map();
    const incomingCount = new Map();

    names.forEach((n) => {
      outgoing.set(n, []);
      incomingCount.set(n, 0);
    });

    Object.entries(edgesConfig).forEach(([from, targets]) => {
      if (!nameSet.has(from)) return;
      (Array.isArray(targets) ? targets : [targets]).forEach((to) => {
        if (!nameSet.has(to)) return;
        if (isBackLink(from, to)) return;
        outgoing.get(from).push(to);
        incomingCount.set(to, incomingCount.get(to) + 1);
      });
    });

    const nodeLevels = new Map();
    let queue = [];

    names.forEach((name) => {
      if (incomingCount.get(name) === 0) {
        nodeLevels.set(name, 0);
        queue.push(name);
      }
    });

    if (queue.length === 0) {
      names.forEach((n) => nodeLevels.set(n, 0));
      queue = [...names];
    }

    let head = 0;
    while (head < queue.length) {
      const current = queue[head++];
      const currentLevel = nodeLevels.get(current);

      (outgoing.get(current) || []).forEach((child) => {
        const childLevel = nodeLevels.get(child) || 0;
        if (currentLevel + 1 > childLevel) {
          nodeLevels.set(child, currentLevel + 1);
          if (!queue.includes(child)) {
            queue.push(child);
          }
        }
      });
    }

    const levels = new Map();
    names.forEach((name) => {
      const lvl = nodeLevels.get(name) || 0;
      if (!levels.has(lvl)) levels.set(lvl, []);
      levels.get(lvl).push(name);
    });

    const maxColCount = Math.max(0, ...Array.from(levels.values(), (arr) => arr.length));
    const maxGraphHeight = maxColCount * V_STEP;

    const pos = new Map();
    Array.from(levels.keys())
      .sort((a, b) => a - b)
      .forEach((level) => {
        const colNodes = levels.get(level);
        const colHeight = colNodes.length * V_STEP;
        const startY = (maxGraphHeight - colHeight) / 2;
        colNodes.forEach((nodeId, i) => {
          pos.set(nodeId, { x: level * H_STEP, y: startY + i * V_STEP });
        });
      });

    const layoutNodes = vertexEntries.map(([key, serviceName]) => {
      const p = pos.get(key) || { x: 0, y: 0 };
      return {
        id: key,
        type: 'serviceNode',
        position: { x: p.x, y: p.y },
        data: { key, label: serviceName },
      };
    });

    const edgeList = [];
    Object.entries(edgesConfig).forEach(([from, targets]) => {
      if (!nameSet.has(from)) return;
      (Array.isArray(targets) ? targets : [targets]).forEach((to) => {
        if (!nameSet.has(to)) return;
        edgeList.push({
          id: `edge:${from}:${to}`,
          source: from,
          target: to,
          animated: true,
          markerEnd: { type: MarkerType.ArrowClosed },
          sourceHandle: 'source-right',
          targetHandle: 'target-left',
        });
      });
    });

    return { layoutNodes, edges: edgeList };
  }, [verticesConfig, edgesConfig]);

  const nodesWithStatus = useMemo(
    () =>
      layoutNodes.map((n) => ({
        ...n,
        data: { ...n.data, status: serviceStatusMap.get(n.data.label) || 'DOWN' },
      })),
    [layoutNodes, serviceStatusMap]
  );

  const [nodes, setNodes] = useState([]);
  const [manualPos, setManualPos] = useState({});

  useEffect(() => {
    setNodes((prev) => {
      const prevMap = new Map(prev.map((n) => [n.id, n]));
      return nodesWithStatus.map((n) => {
        if (manualPos[n.id]) {
          return { ...n, position: manualPos[n.id] };
        }
        const prevNode = prevMap.get(n.id);
        if (prevNode && prevNode.selected) {
          return { ...n, selected: true };
        }
        return n;
      });
    });
  }, [nodesWithStatus, manualPos]);

  const onNodesChange = useCallback((changes) => {
    setNodes((nds) => applyNodeChanges(changes, nds));
    const moved = {};
    changes.forEach((c) => {
      if (c.type === 'position' && c.dragging === false && c.position) {
        moved[c.id] = { x: c.position.x, y: c.position.y };
      }
    });
    if (Object.keys(moved).length > 0) {
      setManualPos((p) => ({ ...p, ...moved }));
    }
  }, []);

  return (
    <section className="section">
      <h2>Services</h2>
      {error && <div className="error-box">Error: {error}</div>}
      {!error && services.length === 0 && (
        <div className="empty-box">No services configured.</div>
      )}
      {!error && services.length > 0 && (
        <div className="services-flow">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={NODE_TYPES}
            onNodesChange={onNodesChange}
            nodesConnectable={false}
            edgesReconnectable={false}
            fitView
            fitViewOptions={{ padding: 0.15 }}
            proOptions={{ hideAttribution: true }}
          >
            <Background />
            <Controls />
          </ReactFlow>
        </div>
      )}
      {edgesError && <div className="empty-box">Edges error: {edgesError}</div>}
      {verticesError && <div className="empty-box">Vertices error: {verticesError}</div>}
      <ul className="services-list">
        {!error &&
          services.map((service) => (
            <li key={service.name} className="service-card">
              <div className="service-card-header">
                <span className="service-name">{service.name}</span>
                <span
                  className={`status-badge status-badge--${(service.status || 'DOWN').toLowerCase()}`}
                >
                  {service.status}
                </span>
              </div>
              <div className="service-card-body">
                <div>
                  <span className="service-label">URL: </span>
                  <a href={service.url} target="_blank" rel="noreferrer">
                    {service.url}
                  </a>
                </div>
                <div>
                  <span className="service-label">Health check: </span>
                  {service.healthCheckUrl}
                </div>
              </div>
            </li>
          ))}
      </ul>
    </section>
  );
}