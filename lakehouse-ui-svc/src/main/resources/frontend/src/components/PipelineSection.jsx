import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Background, Controls, Handle, MarkerType, Position, ReactFlow } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { fetchScheduleInstanceDAG } from '../api.js';

const ACT_MIN_PERCENT = 50;
const ACT_MAX_PERCENT = 90;
const ACT_DEFAULT_PERCENT = 70;

const TASK_W = 120;
const TASK_X_GAP = 150;
const TASK_Y_GAP = 56;
const ACT_TOP_PAD = 38;
const ACT_SIDE_PAD = 16;
const ACT_BOTTOM_PAD = 16;
const ACT_X_GAP = 90;
const ACT_Y_GAP = 48;
const START_X = 20;
const START_Y = 20;

function computeLayers(ids, edges) {
  const layer = new Map();
  const inDegree = new Map();
  ids.forEach((id) => inDegree.set(id, 0));
  edges.forEach((e) => {
    if (inDegree.has(e.to)) inDegree.set(e.to, inDegree.get(e.to) + 1);
  });
  const queue = [];
  ids.forEach((id) => {
    if (inDegree.get(id) === 0) {
      layer.set(id, 0);
      queue.push(id);
    }
  });
  let qi = 0;
  while (qi < queue.length) {
    const node = queue[qi++];
    edges
      .filter((e) => e.from === node)
      .forEach((e) => {
        const next = e.to;
        const nextLayer = layer.get(node) + 1;
        if (!layer.has(next) || nextLayer > layer.get(next)) {
          layer.set(next, nextLayer);
        }
        inDegree.set(next, inDegree.get(next) - 1);
        if (inDegree.get(next) === 0) {
          queue.push(next);
        }
      });
  }
  return layer;
}

function PipelineActNode({ data }) {
  const statusClass = data.status ? ` dag-act--${String(data.status).toLowerCase()}` : '';
  return (
    <div className={`dag-act${statusClass}`}>
      <Handle type="target" position={Position.Left} className="dag-handle dag-handle--target" />
      <Handle type="source" position={Position.Right} className="dag-handle dag-handle--source" />
      <div className="dag-act-title">{data.label}</div>
    </div>
  );
}

function PipelineTaskNode({ data }) {
  const statusClass = data.status ? ` dag-node--${String(data.status).toLowerCase()}` : '';
  return (
    <div className={`dag-node dag-node--task${statusClass}`}>
      <Handle type="target" position={Position.Left} className="dag-handle dag-handle--target" />
      <Handle type="source" position={Position.Right} className="dag-handle dag-handle--source" />
      {data.label}
    </div>
  );
}

const NODE_TYPES = { pipelineAct: PipelineActNode, pipelineTask: PipelineTaskNode };

function buildLayout(dag) {
  const acts = dag.scenarioActs || [];
  const actEdges = dag.scenarioActEdges || [];

  const actNodeId = new Map(acts.map((act) => [act.name, `act:${act.id}`]));
  const nodes = [];
  const edges = [];

  acts.forEach((act) => {
    const tasks = act.tasks || [];
    const taskNames = tasks.map((t) => t.name);
    const taskLayers = computeLayers(taskNames, act.taskEdges || []);
    const tilesByLayer = new Map();
    taskNames.forEach((name) => {
      const t = tasks.find((x) => x.name === name);
      const l = taskLayers.get(name) || 0;
      if (!tilesByLayer.has(l)) tilesByLayer.set(l, []);
      tilesByLayer.get(l).push(t);
    });
    const layerKeys = [...tilesByLayer.keys()].sort((a, b) => a - b);
    const numCols = layerKeys.length ? Math.max(...layerKeys) + 1 : 1;
    const numRows = Math.max(1, ...Array.from(tilesByLayer.values()).map((arr) => arr.length));

    act._layout = {
      numCols,
      numRows,
      actW: Math.max(190, ACT_SIDE_PAD * 2 + (numCols - 1) * TASK_X_GAP + TASK_W),
      actH: Math.max(110, ACT_TOP_PAD + (numRows - 1) * TASK_Y_GAP + 46 + ACT_BOTTOM_PAD),
      tiles: [],
    };
    layerKeys.forEach((l) => {
      tilesByLayer.get(l).forEach((t, i) => {
        act._layout.tiles.push({
          id: `task:${t.id}`,
          pos: { x: ACT_SIDE_PAD + l * TASK_X_GAP, y: ACT_TOP_PAD + i * TASK_Y_GAP },
          task: t,
        });
      });
    });
  });

  const actLayers = computeLayers(
    acts.map((a) => a.name),
    actEdges
  );
  const actsByLayer = new Map();
  acts.forEach((act) => {
    const l = actLayers.get(act.name) || 0;
    if (!actsByLayer.has(l)) actsByLayer.set(l, []);
    actsByLayer.get(l).push(act);
  });
  const layerKeys = [...actsByLayer.keys()].sort((a, b) => a - b);

  let x = START_X;
  layerKeys.forEach((l) => {
    const arr = actsByLayer.get(l);
    const colWidth = Math.max(...arr.map((a) => a._layout.actW));
    arr.forEach((act, i) => {
      act._layout.pos = { x, y: START_Y + i * (act._layout.actH + ACT_Y_GAP) };
    });
    x += colWidth + ACT_X_GAP;
  });

  acts.forEach((act) => {
    const p = act._layout.pos;
    nodes.push({
      id: `act:${act.id}`,
      type: 'pipelineAct',
      position: p,
      style: { width: act._layout.actW, height: act._layout.actH },
      data: { label: act.name, status: act.status, confDataSetKeyName: act.confDataSetKeyName, act },
      draggable: false,
    });
    act._layout.tiles.forEach((tile) => {
      nodes.push({
        id: tile.id,
        type: 'pipelineTask',
        parentId: `act:${act.id}`,
        extent: 'parent',
        position: tile.pos,
        data: { label: tile.task.name, status: tile.task.status, task: tile.task, act },
        draggable: false,
      });
    });
  });

  const actNodeStatus = new Map(acts.map((act) => [act.name, act.status]));
  const actIsRunning = (e) => actNodeStatus.get(e.to) === 'RUNNING';

  actEdges.forEach((e) => {
    const from = actNodeId.get(e.from);
    const to = actNodeId.get(e.to);
    if (from && to) {
      edges.push({
        id: `edge:${from}:${to}`,
        source: from,
        target: to,
        animated: actIsRunning(e),
        markerEnd: { type: MarkerType.ArrowClosed },
      });
    }
  });

  acts.forEach((act) => {
    const taskNodeId = new Map((act.tasks || []).map((t) => [t.name, `task:${t.id}`]));
    const taskNodeStatus = new Map(act.tasks.map((t) => [t.name, t.status]));
    const taskIsRunning = (te) => taskNodeStatus.get(te.to) === 'RUNNING';
    (act.taskEdges || []).forEach((te) => {
      const from = taskNodeId.get(te.from);
      const to = taskNodeId.get(te.to);
      if (from && to) {
        edges.push({
          id: `edge:${from}:${to}`,
          source: from,
          target: to,
          animated: taskIsRunning(te),
          markerEnd: { type: MarkerType.ArrowClosed },
        });
      }
    });
  });

  return { nodes, edges, actById: new Map() };
}

function DescriptionList({ items }) {
  return (
    <div className="pipeline-description-list">
      {items
        .filter((item) => item.value != null && item.value !== '')
        .map((item) => (
          <div className="pipeline-description-row" key={item.label}>
            <span className="pipeline-description-label">{item.label}</span>
            <span className="pipeline-description-value">{String(item.value)}</span>
          </div>
        ))}
    </div>
  );
}

export default function PipelineSection({ instanceId }) {
  const containerRef = useRef(null);
  const [pilePercent, setPilePercent] = useState(ACT_DEFAULT_PERCENT);
  const [dragging, setDragging] = useState(false);
  const [dag, setDag] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedNode, setSelectedNode] = useState(null);

  useEffect(() => {
    if (!instanceId) {
      setDag(null);
      setSelectedNode(null);
      return;
    }
    setLoading(true);
    setError('');
    fetchScheduleInstanceDAG(instanceId)
      .then((data) => {
        setDag(data);
        setSelectedNode(null);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [instanceId]);

  useEffect(() => {
    if (!dragging) return;
    const handleMove = (e) => {
      const rect = containerRef.current.getBoundingClientRect();
      if (rect.width === 0) return;
      let percent = ((e.clientX - rect.left) / rect.width) * 100;
      percent = Math.min(ACT_MAX_PERCENT, Math.max(ACT_MIN_PERCENT, percent));
      setPilePercent(percent);
    };
    const handleUp = () => setDragging(false);
    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [dragging]);

  const layout = dag ? buildLayout(dag) : { nodes: [], edges: [], actById: new Map() };
  const onNodeClick = useCallback((_, node) => setSelectedNode(node), []);

  const buildDetails = () => {
    if (selectedNode && selectedNode.data) {
      const data = selectedNode.data;
      if (data.act) {
        const act = data.act;
        return {
          title: `Scenario act: ${act.name}`,
          items: [
            { label: 'Id', value: act.id },
            { label: 'Name', value: act.name },
            { label: 'Data set', value: act.confDataSetKeyName },
            { label: 'Status', value: act.status },
            { label: 'Tasks', value: (act.tasks || []).length },
          ],
        };
      }
      if (data.task) {
        const task = data.task;
        return {
          title: `Task: ${task.name}`,
          items: [
            { label: 'Id', value: task.id },
            { label: 'Name', value: task.name },
            { label: 'Begin', value: task.beginDateTime },
            { label: 'End', value: task.endDateTime },
            { label: 'Status', value: task.status },
            { label: 'Retry', value: task.reTryNum },
            { label: 'Service', value: task.serviceId },
            { label: 'Causes', value: task.causes },
          ],
        };
      }
    }
    if (dag) {
      return {
        title: `Schedule: ${dag.configScheduleKeyName}`,
        items: [
          { label: 'Id', value: dag.id },
          { label: 'Schedule', value: dag.configScheduleKeyName },
          { label: 'Target execution time', value: dag.targetExecutionDateTime },
          { label: 'Status', value: dag.status },
          { label: 'Scenario acts', value: (dag.scenarioActs || []).length },
        ],
      };
    }
    return null;
  };

  const details = buildDetails();

  return (
    <section className="pipeline-section">
      <h2>Pipeline</h2>
      <div className="pipeline-layout" ref={containerRef}>
        <div className="pipeline-pane pipeline-pane--graph" style={{ width: `${pilePercent}%` }}>
          {error && <div className="error-box">Error: {error}</div>}
          {loading && <div className="empty-box">Loading...</div>}
          {!error && !loading && !instanceId && (
            <div className="empty-box">Select a schedule run to display the pipeline.</div>
          )}
          {!error && !loading && instanceId && dag && (
            <div className="pipeline-flow">
              <ReactFlow
                nodes={layout.nodes}
                edges={layout.edges}
                nodeTypes={NODE_TYPES}
                fitView
                fitViewOptions={{ padding: 0.15 }}
                proOptions={{ hideAttribution: true }}
                onNodeClick={onNodeClick}
              >
                <Background />
                <Controls />
              </ReactFlow>
            </div>
          )}
        </div>
        <div
          className={`catalog-splitter ${dragging ? 'catalog-splitter--dragging' : ''}`}
          onMouseDown={(e) => {
            e.preventDefault();
            setDragging(true);
          }}
        />
        <div className="pipeline-pane pipeline-pane--details">
          {!details && <div className="empty-box">No pipeline details.</div>}
          {details && (
            <div className="pipeline-details">
              <h3 className="pipeline-details-title">{details.title}</h3>
              <DescriptionList items={details.items} />
            </div>
          )}
        </div>
      </div>
    </section>
  );
}