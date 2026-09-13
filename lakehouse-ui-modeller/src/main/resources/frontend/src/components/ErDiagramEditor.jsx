import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ReactFlow,
  ReactFlowProvider,
  Background,
  BackgroundVariant,
  Controls,
  MarkerType,
  Handle,
  Position,
  applyNodeChanges,
  useReactFlow,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { api, encodePath } from '../api';
import { parseYaml, stringifyYaml } from '../yaml';
import Modal from './Modal';
import FormEditor from './FormEditor';
import { DataSetKeyPickerModal } from './Pickers';

const DEFAULT_COLOR = '#2563eb';
const INVALID_COLOR = '#dc2626';
const MISSING_HANDLE_SOURCE = '__err__:source';
const MISSING_HANDLE_TARGET = '__err__:target';

function parseColumns(value) {
  return String(value == null ? '' : value)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

/** Sorted (alphabetical) comma-joined key of a constraint column list. */
function sortedKey(value) {
  return parseColumns(value).sort().join('\u0000');
}

function hasColumn(dsDoc, name) {
  return Array.isArray(dsDoc && dsDoc.columnSchema)
    && dsDoc.columnSchema.some((c) => c && c.name === name);
}

/**
 * Derives the ReactFlow graph from the diagram document plus the loaded data set
 * documents. Nodes are the placed data sets rendered as column tables; edges are
 * computed from the FOREIGN constraints between the data sets.
 */
function buildGraph(doc, dsByKey) {
  const refs = Array.isArray(doc && doc.dataSets) ? doc.dataSets : [];
  const nodes = [];
  const edges = [];

  for (const ref of refs) {
    if (!ref || !ref.keyName) continue;
    const ds = dsByKey[ref.keyName];
    const dsDoc = ds ? ds.doc : null;
    const columns = (dsDoc && Array.isArray(dsDoc.columnSchema) ? dsDoc.columnSchema : [])
      .filter((c) => c && c.name);
    const subtitle = [dsDoc && dsDoc.dataSourceKeyName, dsDoc && dsDoc.databaseSchemaName, dsDoc && dsDoc.tableName]
      .filter(Boolean)
      .join('.');
    const x = Number(ref.x);
    const y = Number(ref.y);
    nodes.push({
      id: ref.keyName,
      type: 'erNode',
      position: {
        x: Number.isFinite(x) ? x : 0,
        y: Number.isFinite(y) ? y : 0,
      },
      data: { keyName: ref.keyName, subtitle, columns, missing: !ds },
    });
  }

  const nodeIndex = new Set(nodes.map((n) => n.id));

  for (const ref of refs) {
    if (!ref || !ref.keyName) continue;
    const ds = dsByKey[ref.keyName];
    if (!ds || !ds.doc) continue;
    const dsDoc = ds.doc;
    const constraints = dsDoc.constraints && typeof dsDoc.constraints === 'object' ? dsDoc.constraints : {};

    for (const [constraintKey, c] of Object.entries(constraints)) {
      if (!c || typeof c !== 'object') continue;
      const type = c.type == null ? '' : String(c.type).toLowerCase();
      if (type !== 'foreign') continue;
      const reference = c.reference;
      if (!reference || typeof reference !== 'object' || !reference.dataSetKeyName) continue;

      const targetKey = reference.dataSetKeyName;
      const targetDs = dsByKey[targetKey];
      const targetConstraint = targetDs && targetDs.doc && targetDs.doc.constraints
        ? targetDs.doc.constraints[reference.constraintName]
        : null;

      const fkCols = parseColumns(c.columns);
      if (fkCols.length === 0) continue;

      // Validation: the referenced data set must be present on the canvas and must
      // carry a constraint with the same name and the same column set.
      const columnSetsMatch = targetConstraint != null
        && typeof targetConstraint === 'object'
        && sortedKey(targetConstraint.columns) === sortedKey(c.columns);
      const valid = !!targetDs && nodeIndex.has(targetKey) && columnSetsMatch;
      const color = valid ? DEFAULT_COLOR : INVALID_COLOR;
      const suffix = valid ? 'blue' : 'red';

      // Line style: solid when every FK column is not nullable, dashed otherwise.
      const identifying = fkCols.every((col) => {
        const entry = (dsDoc.columnSchema || []).find((x) => x && x.name === col);
        return entry ? !entry.nullable : false;
      });
      const edgeStyle = { stroke: color, strokeWidth: 1.5 };
      if (!identifying) edgeStyle.strokeDasharray = '7 5';

      // Multiplicity on the FK side: One-to-One when the FK column set matches a
      // UNIQUE or PRIMARY constraint, One-to-Many otherwise.
      const own = sortedKey(c.columns);
      let oneToOne = false;
      for (const [otherKey, other] of Object.entries(constraints)) {
        if (otherKey === constraintKey || !other || typeof other !== 'object') continue;
        const otherType = other.type == null ? '' : String(other.type).toLowerCase();
        if (otherType !== 'unique' && otherType !== 'primary') continue;
        if (sortedKey(other.columns) === own) {
          oneToOne = true;
          break;
        }
      }

      const refCols = valid && targetConstraint ? parseColumns(targetConstraint.columns) : [];

      fkCols.forEach((fkCol, i) => {
        const targetCol = refCols[i] || fkCol;
        const sourceHandle = hasColumn(dsDoc, fkCol) ? `${fkCol}:right` : MISSING_HANDLE_SOURCE;
        const targetHandle = targetDs && targetDs.doc && hasColumn(targetDs.doc, targetCol)
          ? `${targetCol}:left`
          : MISSING_HANDLE_TARGET;
        edges.push({
          id: `${ref.keyName}:${fkCol}->${targetKey}:${targetCol}`,
          source: ref.keyName,
          sourceHandle,
          target: targetKey,
          targetHandle,
          markerEnd: { type: MarkerType.ArrowClosed, color, width: 16, height: 16 },
          markerStart: oneToOne ? `er-one-${suffix}` : `er-many-${suffix}`,
          style: edgeStyle,
        });
      });
    }
  }

  return { nodes, edges };
}

/** Custom start markers: "one" tick and crow's foot ("many"), colored per edge. */
function MarkerDefs() {
  return (
    <svg className="er-marker-defs" aria-hidden="true">
      <defs>
        {[
          { suffix: 'blue', color: DEFAULT_COLOR },
          { suffix: 'red', color: INVALID_COLOR },
        ].map(({ suffix, color }) => (
          <React.Fragment key={suffix}>
            <marker
              id={`er-one-${suffix}`}
              markerWidth="11"
              markerHeight="11"
              refX="3"
              refY="5.5"
              orient="auto-start-reverse"
              markerUnits="strokeWidth"
            >
              <line x1="3" y1="2" x2="3" y2="9" stroke={color} strokeWidth="1.5" />
            </marker>
            <marker
              id={`er-many-${suffix}`}
              markerWidth="13"
              markerHeight="13"
              refX="3"
              refY="6.5"
              orient="auto-start-reverse"
              markerUnits="strokeWidth"
            >
              <line x1="3" y1="3" x2="3" y2="10" stroke={color} strokeWidth="1.5" />
              <path d="M3 3 L10 6.5 L3 10" fill="none" stroke={color} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </marker>
          </React.Fragment>
        ))}
      </defs>
    </svg>
  );
}

const nodeTypes = { erNode: ERNode };

function ERNode({ id, data, selected }) {
  const columns = data.columns || [];
  return (
    <div className={`er-node${selected ? ' er-node--selected' : ''}${data.missing ? ' er-node--missing' : ''}`}>
      <Handle type="target" id={MISSING_HANDLE_TARGET} position={Position.Left} className="er-handle er-handle--err" style={{ top: 18 }} />
      <Handle type="source" id={MISSING_HANDLE_SOURCE} position={Position.Right} className="er-handle er-handle--err" style={{ top: 18 }} />
      <div className="er-node-head">
        <div className="er-node-title">{data.keyName}</div>
        {data.subtitle ? <div className="er-node-sub" title={data.subtitle}>{data.subtitle}</div> : null}
        {data.missing ? <div className="er-node-warn">dataset file not found</div> : null}
      </div>
      <div className="er-node-cols">
        <div className="er-col-head">
          <span>Column</span>
          <span>Type</span>
        </div>
        {columns.length === 0 ? (
          <div className="er-col-row"><span className="muted">(no columns)</span></div>
        ) : (
          columns.map((col) => (
            <div key={col.name} className="er-col-row">
              <Handle type="target" id={`${col.name}:left`} position={Position.Left} className="er-handle" />
              <span className="er-col-name" title={col.name}>{col.name}</span>
              <span className="er-col-type" title={col.dataType || ''}>{col.dataType || ''}</span>
              <Handle type="source" id={`${col.name}:right`} position={Position.Right} className="er-handle" />
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default function ErDiagramEditor(props) {
  return (
    <ReactFlowProvider>
      <ErDiagramCanvas {...props} />
    </ReactFlowProvider>
  );
}

function ErDiagramCanvas({
  doc,
  onDocChange,
  readOnly,
  tree,
  wsId,
  token,
  onNotice,
  schemas,
  keyNameEditable = true,
  uniqueByField = {},
  dataSetSummaryProvider = null,
  scriptSummaryProvider = null,
  nameSpaceSummaryProvider = null,
  catalogProviders = {},
}) {
  const rf = useReactFlow();
  const hostRef = useRef(null);

  const [dsByPath, setDsByPath] = useState({});
  const [loading, setLoading] = useState(true);
  const [selectedNodeId, setSelectedNodeId] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
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
    () => (Array.isArray(tree) ? tree.filter((e) => e.kind === 'DataSet') : []),
    [tree],
  );

  const reloadOne = useCallback(async (path) => {
    const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(path)}`, { token });
    const parsed = parseYaml(resp.yaml || '');
    const keyName = parsed && typeof parsed === 'object' ? parsed.keyName : null;
    setDsByPath((prev) => ({
      ...prev,
      [path]: { path, keyName, doc: parsed && typeof parsed === 'object' ? parsed : {}, yaml: resp.yaml },
    }));
  }, [wsId, token]);

  useEffect(() => {
    let alive = true;
    (async () => {
      const map = {};
      for (const entry of dataSetFiles) {
        try {
          const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`, { token });
          const parsed = parseYaml(resp.yaml || '');
          if (!parsed || typeof parsed !== 'object' || !parsed.keyName) continue;
          map[entry.path] = { path: entry.path, keyName: parsed.keyName, doc: parsed, yaml: resp.yaml };
        } catch (e) {
          // unreadable data set file — skipped on the canvas
        }
      }
      if (!alive) return;
      setDsByPath(map);
      setLoading(false);
    })();
    return () => {
      alive = false;
    };
  }, [dataSetFiles, wsId, token]);

  const dsByKey = useMemo(() => {
    const map = {};
    for (const info of Object.values(dsByPath)) {
      if (info && info.keyName && !map[info.keyName]) map[info.keyName] = info;
    }
    return map;
  }, [dsByPath]);

  const dataSetRefs = useMemo(
    () => (Array.isArray(doc && doc.dataSets) ? doc.dataSets : []),
    [doc],
  );
  const placedKeys = useMemo(() => new Set(dataSetRefs.map((r) => r && r.keyName).filter(Boolean)), [dataSetRefs]);

  const { nodes, edges } = useMemo(() => buildGraph(doc, dsByKey), [doc, dsByKey]);
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

  const updatePlacements = useCallback((updater) => {
    onDocChange({ ...doc, dataSets: updater(dataSetRefs) });
  }, [doc, dataSetRefs, onDocChange]);

  const addDataSet = useCallback((keyName) => {
    if (!keyName || placedKeys.has(keyName)) return;
    const center = computeCenter();
    onDocChange({
      ...doc,
      dataSets: [...dataSetRefs, { keyName, x: Math.round(center.x), y: Math.round(center.y) }],
    });
  }, [doc, dataSetRefs, placedKeys, computeCenter, onDocChange]);

  const removeSelected = useCallback(() => {
    if (!selectedNodeId) return;
    updatePlacements((refs) => refs.filter((r) => r && r.keyName !== selectedNodeId));
  }, [selectedNodeId, updatePlacements]);

  const openEdit = useCallback(async () => {
    if (!selectedNodeId) return;
    const entry = dataSetFiles.find((f) => f.keyName === selectedNodeId);
    if (!entry) return;
    setEditBusy(true);
    try {
      const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`, { token });
      setEditYaml(resp.yaml || '');
      setEditDoc(parseYaml(resp.yaml || '') || {});
      setEditKeyNameEditable(resp.isKeyNameEditable !== false);
      setEditMode(dsSchema ? 'form' : 'yaml');
      setEditTarget(selectedNodeId);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setEditBusy(false);
    }
  }, [selectedNodeId, dataSetFiles, wsId, token, dsSchema, onNotice]);

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
      await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`, {
        method: 'PUT',
        token,
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
  }, [editTarget, editMode, editDoc, editYaml, dataSetFiles, wsId, token, reloadOne, onNotice]);

  const onNodesChange = useCallback((changes) => {
    if (readOnly) return;
    const removed = changes.filter((c) => c.type === 'remove');
    if (removed.length > 0) {
      const ids = new Set(removed.map((c) => c.id));
      updatePlacements((refs) => refs.filter((r) => r && r.keyName && !ids.has(r.keyName)));
      return;
    }
    setFlowNodes((nds) => applyNodeChanges(changes, nds));
  }, [readOnly, updatePlacements]);

  const onNodeDragStop = useCallback((event, node) => {
    updatePlacements((refs) => refs.map((r) => (
      r && r.keyName === node.id
        ? { ...r, x: Math.round(node.position.x), y: Math.round(node.position.y) }
        : r
    )));
  }, [updatePlacements]);

  const canEditSelected = selectedNodeId && dataSetFiles.some((f) => f.keyName === selectedNodeId);

  return (
    <div className="er-canvas">
      <div className="er-host" ref={hostRef}>
        <ReactFlow
          nodes={flowNodes}
          edges={flowEdges}
          nodeTypes={nodeTypes}
          onNodesChange={onNodesChange}
          onNodeDragStop={onNodeDragStop}
          onSelectionChange={({ nodes: selNodes }) => setSelectedNodeId(selNodes[0] ? selNodes[0].id : null)}
          nodesDraggable={!readOnly}
          nodesConnectable={false}
          elementsSelectable
          deleteKeyCode={null}
          minZoom={0.2}
          proOptions={{ hideAttribution: true }}
        >
          <Background variant={BackgroundVariant.Dots} gap={18} size={1.2} />
          <Controls position="bottom-right" />
          <MarkerDefs />
        </ReactFlow>
        {!readOnly && (
          <div className="er-toolbar">
            <button type="button" onClick={() => setAddOpen(true)} disabled={dataSetFiles.length === 0}>Add</button>
            <button type="button" onClick={openEdit} disabled={!canEditSelected}>Edit</button>
            <button type="button" className="danger" onClick={removeSelected} disabled={!selectedNodeId}>Remove</button>
          </div>
        )}
        {!loading && dataSetRefs.length === 0 && (
          <div className="er-empty-hint">
            No data sets on the diagram yet.{!readOnly && ' Use "Add" to place one.'}
          </div>
        )}
      </div>

      {addOpen && (
        <DataSetKeyPickerModal
          summaryProvider={async () => dataSetFiles
            .filter((f) => !placedKeys.has(f.keyName))
            .map((f) => ({ keyName: f.keyName }))}
          onClose={() => setAddOpen(false)}
          onApply={addDataSet}
        />
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
              nameSpaceSummaryProvider={nameSpaceSummaryProvider}
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