import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ReactFlow,
  ReactFlowProvider,
  Background,
  BackgroundVariant,
  Controls,
  Handle,
  MarkerType,
  Position,
  SmoothStepEdge,
  applyNodeChanges,
  useReactFlow,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { api, encodePath } from '../api.js';
import { parseYaml, stringifyYaml } from '../yaml.js';
import Modal from './Modal';
import FormEditor from './FormEditor';
import { DataSetKeyPickerModal } from './Pickers';

const DEFAULT_COLOR = '#2563eb';
const NODE_WIDTH = 220;
const NODE_GAP_X = 90;
const NODE_GAP_Y = 36;
const FALLBACK_COLOR = '#6b7280';

/** Corner radius of the lineage connectors, in pixels. */
export const EDGE_CORNER_RADIUS = 16;
/** Distance of the connector step from the source/target handle. */
export const EDGE_STEP_OFFSET = 20;
/** Name of the custom edge type that draws the rounded connectors. */
export const EDGE_TYPE = 'lineageEdge';

function asNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

function asPositions(layout) {
  return layout && typeof layout.positions === 'object' && layout.positions ? layout.positions : {};
}

/**
 * Derives the ReactFlow graph from the lineage diagram document plus the loaded
 * DataSet documents. Nodes come from `doc.spec.datasets`; edges are computed from
 * each DataSet `sources` map intersected with the datasets on the canvas, flowing
 * left to right (target dataset on the left via its `target` handle).
 */
export function buildLineageGraph(doc, dsByKey) {
  const spec = doc && typeof doc.spec === 'object' && doc.spec ? doc.spec : {};
  const datasets = Array.isArray(spec.datasets)
    ? spec.datasets.map((key) => (key == null ? '' : String(key))).filter(Boolean)
    : [];
  const layout = spec.layout && typeof spec.layout === 'object' ? spec.layout : {};
  const positions = asPositions(layout);

  const nodes = datasets.map((keyName, index) => {
    const stored = positions[keyName] && typeof positions[keyName] === 'object' ? positions[keyName] : null;
    const x = stored ? asNumber(stored.x) : null;
    const y = stored ? asNumber(stored.y) : null;
    const info = dsByKey[keyName];
    const dsDoc = info ? info.doc : null;
    const subtitle = [dsDoc && dsDoc.dataSourceKeyName, dsDoc && dsDoc.databaseSchemaName, dsDoc && dsDoc.tableName]
      .filter(Boolean)
      .join('.');
    return {
      id: keyName,
      type: 'lineageNode',
      position: {
        x: x != null ? x : index * (NODE_WIDTH + NODE_GAP_X),
        y: y != null ? y : 0,
      },
      data: { keyName, subtitle, missing: !info, dsDoc },
    };
  });

  const nodeIds = new Set(nodes.map((node) => node.id));
  const edges = [];
  const outgoing = {};
  const incoming = {};
  for (const keyName of datasets) {
    outgoing[keyName] = 0;
    incoming[keyName] = 0;
  }

  for (const keyName of datasets) {
    const info = dsByKey[keyName];
    const dsDoc = info ? info.doc : null;
    if (!dsDoc) continue;
    const sources = dsDoc.sources && typeof dsDoc.sources === 'object' ? dsDoc.sources : {};
    for (const sourceKey of Object.keys(sources)) {
      if (sourceKey === keyName) continue;
      if (!nodeIds.has(sourceKey)) continue;
      edges.push({
        id: `${sourceKey}->${keyName}`,
        source: sourceKey,
        target: keyName,
        type: EDGE_TYPE,
        markerEnd: { type: MarkerType.ArrowClosed, color: DEFAULT_COLOR, width: 16, height: 16 },
        style: {
          stroke: DEFAULT_COLOR,
          strokeWidth: 1.5,
          strokeLinecap: 'round',
          strokeLinejoin: 'round',
        },
      });
      outgoing[keyName] += 1;
      incoming[sourceKey] += 1;
    }
  }

  for (const node of nodes) {
    node.data.incoming = incoming[node.id] || 0;
    node.data.outgoing = outgoing[node.id] || 0;
  }

  return { nodes, edges };
}

/**
 * Returns a shallow copy of the diagram document with the given node position
 * written into `doc.spec.layout.positions[keyName]` (rounded to whole pixels).
 */
export function setNodePosition(doc, keyName, position) {
  const spec = doc && typeof doc.spec === 'object' && doc.spec ? doc.spec : {};
  const layout = spec.layout && typeof spec.layout === 'object' ? spec.layout : {};
  const positions = asPositions(layout);
  return {
    ...doc,
    spec: {
      ...spec,
      layout: {
        ...layout,
        positions: {
          ...positions,
          [keyName]: { x: Math.round(position.x), y: Math.round(position.y) },
        },
      },
    },
  };
}

/**
 * Returns a copy of the diagram document with the given data set appended to
 * `spec.datasets` (when it is not already present) and placed at the end of the
 * default horizontal flow.
 */
export function addLineageDataset(doc, keyName) {
  if (!keyName) return doc;
  const spec = doc && typeof doc.spec === 'object' && doc.spec ? doc.spec : {};
  const datasets = Array.isArray(spec.datasets)
    ? spec.datasets.map((key) => (key == null ? '' : String(key))).filter(Boolean)
    : [];
  if (datasets.indexOf(keyName) >= 0) return doc;
  const layout = spec.layout && typeof spec.layout === 'object' ? spec.layout : {};
  return {
    ...doc,
    spec: {
      ...spec,
      datasets: [...datasets, keyName],
      layout: {
        ...layout,
        positions: {
          ...asPositions(layout),
          [keyName]: { x: datasets.length * (NODE_WIDTH + NODE_GAP_X), y: 0 },
        },
      },
    },
  };
}

/**
 * Returns a copy of the diagram document with the given data set removed from
 * `spec.datasets` together with its stored canvas position.
 */
export function removeLineageDataset(doc, keyName) {
  if (!keyName) return doc;
  const spec = doc && typeof doc.spec === 'object' && doc.spec ? doc.spec : {};
  const datasets = Array.isArray(spec.datasets) ? spec.datasets : [];
  if (!datasets.includes(keyName)) return doc;
  const layout = spec.layout && typeof spec.layout === 'object' ? spec.layout : {};
  const nextPositions = {};
  for (const key of Object.keys(asPositions(layout))) {
    if (key !== keyName) nextPositions[key] = asPositions(layout)[key];
  }
  return {
    ...doc,
    spec: {
      ...spec,
      datasets: datasets.filter((key) => key !== keyName),
      layout: { ...layout, positions: nextPositions },
    },
  };
}

const nodeTypes = { lineageNode: LineageNode };

/**
 * The built-in `smoothstep` edge type hardcodes `borderRadius: 0` and drops the
 * radius of the caller, so the connectors are drawn by a custom edge that keeps the
 * orthogonal step routing but rounds the corners and the line caps.
 */
function LineageEdge({ pathOptions, ...props }) {
  return (
    <SmoothStepEdge
      {...props}
      pathOptions={{ ...pathOptions, borderRadius: EDGE_CORNER_RADIUS, offset: EDGE_STEP_OFFSET }}
    />
  );
}

const edgeTypes = { [EDGE_TYPE]: LineageEdge };

function LineageNode({ data, selected }) {
  return (
    <div className={`lineage-node${selected ? ' lineage-node--selected' : ''}${data.missing ? ' lineage-node--missing' : ''}`}>
      <Handle type="target" position={Position.Left} className="lineage-handle lineage-handle--left" />
      <div className="lineage-node-head">
        <div className="lineage-node-title" title={data.keyName}>{data.keyName}</div>
        <div className="lineage-node-kind">DataSet</div>
      </div>
      {data.subtitle ? <div className="lineage-node-sub" title={data.subtitle}>{data.subtitle}</div> : null}
      {data.missing ? <div className="lineage-node-warn">dataset file not found</div> : null}
      <div className="lineage-node-foot">
        <span className="muted">{data.incoming} in</span>
        <span className="muted">{data.outgoing} out</span>
      </div>
      <Handle type="source" position={Position.Right} className="lineage-handle lineage-handle--right" />
    </div>
  );
}

export default function DataLineageDiagramEditor(props) {
  return (
    <ReactFlowProvider>
      <LineageCanvas {...props} />
    </ReactFlowProvider>
  );
}

function LineageCanvas({
  doc,
  onChange,
  readOnly,
  tree,
  wsId,
  onNotice,
  schemas,
  uniqueByField,
  dataSetSummaryProvider,
  scriptSummaryProvider,
  catalogProviders,
}) {
  const rf = useReactFlow();
  const hostRef = useRef(null);

  const [dsByKey, setDsByKey] = useState({});
  const [loading, setLoading] = useState(true);
  const [selectedKeyName, setSelectedKeyName] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const [removeTarget, setRemoveTarget] = useState(null);
  const [expanded, setExpanded] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [editYaml, setEditYaml] = useState('');
  const [editDoc, setEditDoc] = useState(null);
  const [editMode, setEditMode] = useState('form');
  const [editKeyNameEditable, setEditKeyNameEditable] = useState(true);
  const [editBusy, setEditBusy] = useState(false);

  const dsSchema = useMemo(
    () => (Array.isArray(schemas) ? schemas.find((s) => s && s.kind === 'DataSet') || null : null),
    [schemas],
  );

  const dataSetFiles = useMemo(
    () => (Array.isArray(tree) ? tree.filter((entry) => entry && entry.kind === 'DataSet') : []),
    [tree],
  );

  useEffect(() => {
    let alive = true;
    (async () => {
      const map = {};
      for (const entry of dataSetFiles) {
        try {
          const resp = await api(`/api/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`);
          const parsed = parseYaml(resp.yaml || '');
          if (!parsed || typeof parsed !== 'object' || !parsed.keyName) continue;
          map[parsed.keyName] = { path: entry.path, keyName: parsed.keyName, doc: parsed, yaml: resp.yaml };
        } catch (e) {
          // unreadable data set file — referenced datasets render as missing nodes
        }
      }
      if (!alive) return;
      setDsByKey(map);
      setLoading(false);
    })();
    return () => {
      alive = false;
    };
  }, [dataSetFiles, wsId]);

  const { nodes, edges } = useMemo(() => buildLineageGraph(doc, dsByKey), [doc, dsByKey]);
  const nodesSig = JSON.stringify(nodes);
  const edgesSig = JSON.stringify(edges);

  const [flowNodes, setFlowNodes] = useState([]);
  const [flowEdges, setFlowEdges] = useState([]);

  useEffect(() => {
    setFlowNodes(nodes);
  }, [nodesSig]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    setFlowEdges(edges);
  }, [edgesSig]); // eslint-disable-line react-hooks/exhaustive-deps

  const onNodesChange = useCallback((changes) => {
    if (readOnly) return;
    const removed = changes.filter((c) => c.type === 'remove');
    if (removed.length > 0) return;
    setFlowNodes((nodesList) => applyNodeChanges(changes, nodesList));
  }, [readOnly]);

  const onNodeDragStop = useCallback((event, node) => {
    if (readOnly || !onChange) return;
    onChange(setNodePosition(doc, node.id, node.position));
  }, [doc, readOnly, onChange]);

  const onSelectionChange = useCallback((selection) => {
    const nodes = selection && selection.nodes ? selection.nodes : [];
    setSelectedKeyName(nodes.length > 0 ? nodes[0].id : null);
  }, []);

  const specDatasets = useMemo(
    () => (Array.isArray(doc && doc.spec && doc.spec.datasets) ? doc.spec.datasets : []),
    [doc],
  );
  const totalPlaced = Object.keys(dsByKey).length;
  const placedKeys = useMemo(() => new Set(specDatasets.map((key) => String(key))), [specDatasets]);

  const dataSetChoices = useMemo(() => Object.keys(dsByKey).sort(), [dsByKey]);
  const selectedResolved = selectedKeyName && dsByKey[selectedKeyName] ? selectedKeyName : null;

  const computeCenter = useCallback(() => {
    const el = hostRef.current;
    if (!el) return { x: 200, y: 200 };
    const rect = el.getBoundingClientRect();
    const point = rf.screenToFlowPosition({
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2,
    });
    return { x: point.x, y: point.y };
  }, [rf]);

  const addDataSet = useCallback((keyName) => {
    if (readOnly || !keyName || !onChange || placedKeys.has(keyName)) return;
    const center = computeCenter();
    onChange(setNodePosition(addLineageDataset(doc, keyName), keyName, {
      x: Math.round(center.x),
      y: Math.round(center.y),
    }));
  }, [readOnly, onChange, placedKeys, computeCenter, doc]);

  const confirmRemove = useCallback(() => {
    if (readOnly || !onChange || !removeTarget) return;
    onChange(removeLineageDataset(doc, removeTarget));
    setRemoveTarget(null);
    setSelectedKeyName(null);
  }, [readOnly, onChange, removeTarget, doc]);

  const reloadOne = useCallback(async (path) => {
    const resp = await api(`/api/workspaces/${encodePath(wsId)}/files/${encodePath(path)}`);
    const parsed = parseYaml(resp.yaml || '');
    if (!parsed || typeof parsed !== 'object' || !parsed.keyName) return;
    setDsByKey((prev) => ({
      ...prev,
      [parsed.keyName]: { path, keyName: parsed.keyName, doc: parsed, yaml: resp.yaml },
    }));
  }, [wsId]);

  const openEdit = useCallback(async () => {
    if (!selectedResolved) return;
    const entry = dataSetFiles.find((f) => f.keyName === selectedResolved);
    if (!entry) return;
    setEditBusy(true);
    try {
      const resp = await api(`/api/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`);
      setEditYaml(resp.yaml || '');
      setEditDoc(parseYaml(resp.yaml || '') || {});
      setEditKeyNameEditable(resp.isKeyNameEditable !== false);
      setEditMode(dsSchema ? 'form' : 'yaml');
      setEditTarget(selectedResolved);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setEditBusy(false);
    }
  }, [selectedResolved, dataSetFiles, wsId, dsSchema, onNotice]);

  const switchEditMode = useCallback(() => {
    if (editMode === 'form') {
      setEditYaml(stringifyYaml(editDoc || {}));
      setEditMode('yaml');
    } else {
      setEditDoc(parseYaml(editYaml) || {});
      setEditMode('form');
    }
  }, [editMode, editDoc, editYaml]);

  const saveEdit = useCallback(async () => {
    if (!editTarget) return;
    const entry = dataSetFiles.find((f) => f.keyName === editTarget);
    if (!entry) return;
    setEditBusy(true);
    try {
      const yamlToSave = editMode === 'form' ? stringifyYaml(editDoc || {}) : editYaml;
      await api(`/api/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`, {
        method: 'PUT',
        body: { path: entry.path, yaml: yamlToSave, keyName: editTarget },
      });
      await reloadOne(entry.path);
      setEditTarget(null);
      onNotice('success', `Saved data set ${editTarget}.`);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setEditBusy(false);
    }
  }, [editTarget, editMode, editDoc, editYaml, dataSetFiles, wsId, reloadOne, onNotice]);

  const summaryProvider = useCallback(
    async () => dataSetChoices
      .filter((key) => !placedKeys.has(key))
      .map((key) => ({ keyName: key })),
    [dataSetChoices, placedKeys],
  );

  return (
    <div className="lineage-canvas">
      <div className={`lineage-host${expanded ? ' diagram-fullscreen' : ''}`} ref={hostRef}>
        <ReactFlow
          className="lineage-flow"
          nodes={flowNodes}
          edges={flowEdges}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          onNodesChange={onNodesChange}
          onNodeDragStop={onNodeDragStop}
          onSelectionChange={onSelectionChange}
          nodesDraggable={!readOnly}
          nodesConnectable={false}
          elementsSelectable
          deleteKeyCode={null}
          minZoom={0.2}
          proOptions={{ hideAttribution: true }}
        >
          <Background variant={BackgroundVariant.Dots} gap={18} size={1.2} />
          <Controls position="bottom-right" />
        </ReactFlow>
        {!readOnly && (
          <div className="lineage-toolbar">
            <button
              type="button"
              onClick={() => setAddOpen(true)}
              disabled={dataSetChoices.length === 0}
            >Add</button>
            <button type="button" onClick={openEdit} disabled={!selectedResolved}>Edit</button>
            <button
              type="button"
              className="danger"
              onClick={() => setRemoveTarget(selectedResolved)}
              disabled={!selectedResolved}
            >Remove</button>
          </div>
        )}
        <button
          type="button"
          className={`diagram-expand${expanded ? ' diagram-expand--on' : ''}`}
          title={expanded ? 'Restore diagram size' : 'Expand diagram to full screen'}
          onClick={() => setExpanded((v) => !v)}
        >{expanded ? '>-<' : '<-->'}</button>
        {loading ? (
          <div className="lineage-loading">Loading data sets…</div>
        ) : specDatasets.length === 0 ? (
          <div className="lineage-empty-hint">
            No data sets on the diagram yet.{!readOnly && ' Use "Add" to place one.'}
          </div>
        ) : totalPlaced === 0 ? (
          <div className="lineage-empty-hint">
            No DataSet files found in this workspace, so referenced nodes cannot be resolved.
          </div>
        ) : null}
      </div>

      {addOpen && (
        <DataSetKeyPickerModal
          summaryProvider={summaryProvider}
          onClose={() => setAddOpen(false)}
          onApply={addDataSet}
        />
      )}

      {removeTarget && (
        <Modal title="Remove data set" onClose={() => setRemoveTarget(null)}>
          <p>
            Remove <strong>{removeTarget}</strong> from this diagram?
          </p>
          <div className="btn-row">
            <button className="danger" autoFocus disabled={readOnly} onClick={confirmRemove}>Remove</button>
            <button type="button" onClick={() => setRemoveTarget(null)}>Cancel</button>
          </div>
        </Modal>
      )}

      {editTarget && (
        <Modal title={`kind: DataSet · ${editTarget}`} onClose={() => setEditTarget(null)} wide>
          <div className="btn-row er-edit-head">
            {!readOnly && dsSchema && (
              <button type="button" onClick={switchEditMode}>
                {editMode === 'form' ? 'Raw YAML' : 'Form view'}
              </button>
            )}
          </div>
          {editMode === 'form' && dsSchema ? (
            <FormEditor
              schema={dsSchema}
              value={editDoc || {}}
              onChange={setEditDoc}
              readOnly={readOnly}
              keyNameEditable={editKeyNameEditable && !readOnly}
              uniqueByField={uniqueByField}
              dataSetSummaryProvider={dataSetSummaryProvider}
              scriptSummaryProvider={scriptSummaryProvider}
              catalogProviders={catalogProviders}
            />
          ) : (
            <div className="field">
              <span className="label">YAML</span>
              <textarea
                className="yaml-area er-edit-yaml"
                spellCheck="false"
                rows={20}
                readOnly={readOnly}
                value={editYaml}
                onChange={(e) => setEditYaml(e.target.value)}
              />
            </div>
          )}
          <div className="btn-row">
            <button className="primary" disabled={editBusy} onClick={saveEdit}>Save</button>
            <button type="button" onClick={() => setEditTarget(null)}>Cancel</button>
          </div>
        </Modal>
      )}
    </div>
  );
}