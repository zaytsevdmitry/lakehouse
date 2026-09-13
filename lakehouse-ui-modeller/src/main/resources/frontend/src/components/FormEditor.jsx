import React, { useRef, useState } from 'react';
import DagEditor from './DagEditor';
import Modal from './Modal';
import CodeEditor from './CodeEditor';
import {
  ColumnPickerModal,
  ConstraintPickerModal,
  DataSetKeyPickerModal,
  NamedItemPickerModal,
  NameSpacePickerModal,
  ScriptKeyPreviewModal,
} from './Pickers';

/**
 * Tabbed, schema-driven editor. `value` is a JSON node (Jackson-compatible).
 * Changes are applied immutably through `onChange(nextValue)`.
 *
 * Top-level scalar fields live on the "General" tab; every nested element
 * (list / map / object / dag) gets its own tab. List and map elements are
 * rendered as tables with a + / - / * control panel and an edit dialog.
 */
const SCALAR_TYPES = new Set(['string', 'integer', 'double', 'boolean', 'datetime', 'code']);

const FormContext = React.createContext({
  uniqueByField: {},
  rootDoc: null,
  dataSetSummaryProvider: null,
  scriptSummaryProvider: null,
  nameSpaceSummaryProvider: null,
  catalogProviders: {},
});

/** Catalog pickers for read-only reference fields, keyed by the `picker` value. */
const NAMED_ITEM_PICKERS = {
  dataSource: {
    title: 'Choose data source',
    placeholder: 'Search data source key name…',
    idField: 'keyName',
  },
  task: {
    title: 'Choose template task',
    placeholder: 'Search task name…',
    idField: 'name',
  },
  taskExecutionServiceGroup: {
    title: 'Choose task execution service group',
    placeholder: 'Search task execution service group name…',
    idField: 'name',
  },
  driver: {
    title: 'Choose driver',
    placeholder: 'Search driver key name…',
    idField: 'keyName',
  },
};

export default function FormEditor({
  schema,
  value,
  onChange,
  readOnly,
  doc,
  keyNameEditable = true,
  uniqueByField = {},
  dataSetSummaryProvider = null,
  scriptSummaryProvider = null,
  nameSpaceSummaryProvider = null,
  catalogProviders = {},
}) {
  const fields = schema.fields || [];
  const scalarFields = fields.filter((f) => SCALAR_TYPES.has(f.type));
  const nestedFields = fields.filter((f) => !SCALAR_TYPES.has(f.type));
  const tabs = [
    { id: '__general__', label: 'General', field: null },
    ...nestedFields.map((f) => ({ id: f.name, label: f.label || f.name, field: f })),
  ];
  const [active, setActive] = useState('__general__');
  const tab = tabs.find((t) => t.id === active) || tabs[0];
  const root = value || {};
  const baseChange = (next) => {
    onChange(schema.kind === 'DataSet' ? normalizeDataSetConstraints(next) : next);
  };

  return (
    <FormContext.Provider value={{ uniqueByField, rootDoc: root, dataSetSummaryProvider, scriptSummaryProvider, nameSpaceSummaryProvider, catalogProviders }}>
      <div className="form-editor">
      <div className="tab-list">
        {tabs.map((t) => (
          <button
            key={t.id}
            className={`tab${tab.id === t.id ? ' tab--active' : ''}`}
            onClick={() => setActive(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div className="tab-content">
        {tab.field ? (
          <NestedTab
            field={tab.field}
            value={root[tab.field.name]}
            onChange={(next) => baseChange({ ...root, [tab.field.name]: next })}
            rootDoc={root}
            onBaseChange={baseChange}
            readOnly={readOnly}
            kind={schema.kind}
          />
        ) : (
          <div className="tab-general">
            {scalarFields.length === 0 && <p className="muted">No simple fields.</p>}
            {scalarFields.map((field) => (
              <FieldRow
                key={field.name}
                field={field}
                value={root}
                onChange={baseChange}
                doc={root}
                onBaseChange={baseChange}
                readOnly={readOnly}
                keyNameEditable={keyNameEditable}
              />
            ))}
          </div>
        )}
      </div>
    </div>
    </FormContext.Provider>
  );
}

// ----------------------------------------------------------------------
// tab content for nested fields
// ----------------------------------------------------------------------

function NestedTab({ field, value, onChange, readOnly, rootDoc, onBaseChange, kind }) {
  if (field.type === 'dag') {
    return (
      <div className="field-block">
        <DagEditor
          nodeField={siblingNodeField(rootDoc, field.name)}
          edgeField={field.name}
          doc={rootDoc}
          onDocChange={onBaseChange}
          readOnly={readOnly}
        />
      </div>
    );
  }

  if (field.type === 'object') {
    return (
      <div className="box">
        {field.children && field.children.length > 0 ? (
          field.children.map((child) => (
            <FieldRow
              key={child.name}
              field={child}
              value={value || {}}
              onChange={onChange}
              doc={rootDoc}
              onBaseChange={onBaseChange}
              readOnly={readOnly}
            />
          ))
        ) : (
          <p className="muted">(empty)</p>
        )}
      </div>
    );
  }

  if (field.type === 'list') {
    const entries = Array.isArray(value) ? value : [];
    const item = field.item || {};
    if (item.type === 'object' && item.children && item.children.length > 0)
      return (
        <ObjectTable
          field={field}
          columns={item.children}
          entries={entries}
          onChange={onChange}
          readOnly={readOnly}
        />
      );
    const valueField = { name: '__value__', type: item.type || 'string', label: item.label || 'Value' };
    return (
      <SimpleTable
        field={field}
        columns={[valueField]}
        rows={entries.map((r) => ({ __value__: r }))}
        toEntry={(row) => row.__value__}
        readOnly={readOnly}
        needKey={false}
      />
    );
  }

  if (field.type === 'map') {
    if (kind === 'DataSet' && field.name === 'sources') {
      return <SourceMapEditor field={field} value={value} onChange={onChange} readOnly={readOnly} />;
    }
    const obj = value && typeof value === 'object' ? value : {};
    const children = field.children || [];
    const isObjectMap = children.length > 1 || (children.length === 1 && children[0].name !== 'value');
    if (isObjectMap) {
      const columns = [
        { name: '__key__', type: 'string', label: 'Key' },
        ...children,
      ];
      return (
        <ObjectTable
          field={field}
          columns={columns}
          keyed
          entries={Object.keys(obj).map((k) => ({ __key__: k, ...obj[k] }))}
          toKey={(row) => row.__key__}
          onChange={(rows) => {
            const next = {};
            for (const row of rows) if (row.__key__ && row.__key__.trim()) next[row.__key__] = stripKey(row);
            onChange(next);
          }}
          readOnly={readOnly}
        />
      );
    }
    const valueField = { name: '__value__', type: (children[0] || {}).type || 'string', label: 'Value' };
    return (
      <SimpleTable
        field={field}
        columns={[{ name: '__key__', type: 'string', label: 'Key' }, valueField]}
        rows={Object.keys(obj).map((k) => ({ __key__: k, __value__: obj[k] }))}
        toEntry={(row) => row.__value__}
        onChange={(rows) => {
          const next = {};
          for (const row of rows) if (row.__key__ && row.__key__.trim()) next[row.__key__] = row.__value__;
          onChange(next);
        }}
        readOnly={readOnly}
        needKey
      />
    );
  }

  return null;
}

function stripKey(row) {
  const next = { ...row };
  delete next.__key__;
  return next;
}

// ----------------------------------------------------------------------
// DataSet Sources: two-pane editor (Keys list | Properties table)
// ----------------------------------------------------------------------

/**
 * Split editor for the DataSet `sources` map (Key -> { properties }).
 * The left pane lists the map keys with its own + / - / * panel and a filter;
 * the right pane shows the selected key's `properties` as a filterable table.
 */
function SourceMapEditor({ field, value, onChange, readOnly }) {
  const obj = value && typeof value === 'object' ? value : {};
  const { dataSetSummaryProvider } = React.useContext(FormContext);
  const keys = Object.keys(obj);
  const [sel, setSel] = useState(null);
  const [keyFilter, setKeyFilter] = useState('');
  const [dlg, setDlg] = useState(null);
  const [confirmRemove, setConfirmRemove] = useState(false);

  const q = keyFilter.trim().toLowerCase();
  const displayedKeys = keys.filter((k) => !q || k.toLowerCase().includes(q));

  const submitKey = ({ key }) => {
    const k = key ? key.trim() : '';
    if (!k) return;
    const next = {};
    for (const kk of keys) if (kk !== (dlg && dlg.key)) next[kk] = obj[kk];
    const existing = (dlg && dlg.key && obj[dlg.key]) || {};
    const props = existing.properties && typeof existing.properties === 'object' ? existing.properties : {};
    next[k] = { properties: props };
    setDlg(null);
    setSel(null);
    onChange(next);
  };

  const doRemove = () => {
    setConfirmRemove(false);
    const next = {};
    for (const k of keys) if (k !== sel) next[k] = obj[k];
    setSel(null);
    onChange(next);
  };

  const setKeyProps = (rows) => {
    if (sel == null) return;
    const map = {};
    for (const r of rows) if (r.__key__ && r.__key__.trim()) map[r.__key__] = r.__value__;
    onChange({ ...obj, [sel]: { ...obj[sel], properties: map } });
  };

  return (
    <div className="source-map">
      <div className="source-pane source-pane--keys">
        <div className="pane-head">
          <span className="label">Key</span>
          {!readOnly && (
            <TableToolbar
              canEdit={sel != null}
              onAdd={() => setDlg({ key: null })}
              onEdit={() => setDlg({ key: sel })}
              onRemove={() => setConfirmRemove(true)}
            />
          )}
          <input
            className="filter-input table-filter"
            type="text"
            placeholder="Filter by key…"
            value={keyFilter}
            onChange={(e) => { setKeyFilter(e.target.value); setSel(null); }}
          />
        </div>
        {keys.length === 0 ? (
          <p className="muted empty-hint">(empty)</p>
        ) : displayedKeys.length === 0 ? (
          <p className="muted empty-hint">No matches for "{keyFilter.trim()}".</p>
        ) : (
          <div className="source-key-list">
            {displayedKeys.map((k) => (
              <button
                key={k}
                className={`source-key-row${sel === k ? ' selected' : ''}`}
                title={k}
                onClick={() => setSel(sel === k ? null : k)}
              >
                {k}
              </button>
            ))}
          </div>
        )}
      </div>
      <div className="source-pane source-pane--props">
        {sel == null ? (
          <p className="muted empty-hint">Select a key to edit its properties.</p>
        ) : (
          <SimpleTable
            field={{ name: 'properties', label: 'Properties' }}
            columns={[
              { name: '__key__', type: 'string', label: 'Key' },
              { name: '__value__', type: 'string', label: 'Value' },
            ]}
            needKey
            rows={Object.keys(obj[sel].properties || {}).map((k) => ({
              __key__: k,
              __value__: obj[sel].properties[k],
            }))}
            toEntry={(r) => r.__value__}
            onChange={setKeyProps}
            readOnly={readOnly}
          />
        )}
      </div>
      {dlg && (
        <SourceKeyDialog
          existingKey={dlg.key}
          summaryProvider={dataSetSummaryProvider}
          onCancel={() => setDlg(null)}
          onSave={submitKey}
        />
      )}
      {confirmRemove && (
        <Modal title="Remove key" onClose={() => setConfirmRemove(false)}>
          <p>Remove key "{sel}" and its properties?</p>
          <div className="btn-row">
            <button className="danger" onClick={doRemove}>Yes</button>
            <button onClick={() => setConfirmRemove(false)}>Cancel</button>
          </div>
        </Modal>
      )}
    </div>
  );
}

/**
 * Add / edit modal for a source key. The Key field is read-only and filled via
 * the "…" data set picker; Save persists the key name, Cancel leaves it alone.
 */
function SourceKeyDialog({ existingKey, summaryProvider, onCancel, onSave }) {
  const [key, setKey] = useState(existingKey || '');
  const [pickerOpen, setPickerOpen] = useState(false);

  return (
    <Modal title={existingKey ? 'Edit key' : 'Add key'} onClose={onCancel}>
      <div className="field">
        <span className="label">Key</span>
        <div className="key-picker-field">
          <input value={key} readOnly placeholder="Choose a data set key name" />
          <button className="square picker-btn" title="Choose data set" onClick={() => setPickerOpen(true)}>…</button>
        </div>
      </div>
      <div className="btn-row">
        <button className="primary" disabled={!key.trim()} onClick={() => onSave({ key })}>Save</button>
        <button onClick={onCancel}>Cancel</button>
      </div>
      {pickerOpen && summaryProvider && (
        <DataSetKeyPickerModal
          summaryProvider={summaryProvider}
          onApply={setKey}
          onClose={() => setPickerOpen(false)}
        />
      )}
    </Modal>
  );
}

// ----------------------------------------------------------------------
// shared table machinery
// ----------------------------------------------------------------------

function TableToolbar({ onAdd, onEdit, onRemove, canEdit }) {
  return (
    <div className="table-toolbar">
      <button className="square primary" title="Add" onClick={onAdd}>+</button>
      <button className="square danger" title="Remove" disabled={!canEdit} onClick={onRemove}>-</button>
      <button className="square" title="Edit" disabled={!canEdit} onClick={onEdit}>*</button>
    </div>
  );
}

function cellValue(v) {
  if (v === null || v === undefined || v === '') return '';
  if (typeof v === 'boolean') return v ? 'true' : 'false';
  if (typeof v === 'object') return JSON.stringify(v);
  return String(v);
}

/**
 * Table over a list (or keyed map flatten) of objects. Columns are the children
 * of the item schema; for keyed maps a synthetic `__key__` column is the Map key.
 */
function ObjectTable({ field, columns, entries, onChange, readOnly, keyed = false, toKey }) {
  const cols = columns || field.children || [];
  const [sel, setSel] = useState(null);
  const [dlg, setDlg] = useState(null);
  const [confirmRemove, setConfirmRemove] = useState(false);
  const canModify = !readOnly;
  const keyOf = keyed ? toKey : (row) => row;

  const submit = (row) => {
    const key = row.__key__ ? row.__key__.trim() : null;
    if (keyed && !key) return;
    setDlg(null);
    let next;
    if (dlg && dlg.index < 0) {
      next = [...entries, row];
    } else {
      next = entries.map((r, i) => (i === dlg.index ? row : r));
    }
    onChange(next);
    setSel(null);
  };

  const remove = () => {
    if (sel == null) return;
    setConfirmRemove(true);
  };

  const doRemove = () => {
    setConfirmRemove(false);
    let next;
    if (keyed) {
      next = entries.filter((r) => r.__key__ !== sel);
    } else {
      next = entries.filter((_, i) => i !== sel);
    }
    onChange(next);
    setSel(null);
  };

  const edit = () => {
    if (sel == null) return;
    const index = keyed ? entries.findIndex((r) => r.__key__ === sel) : sel;
    if (index < 0) return;
    setDlg({ index, row: { ...entries[index] } });
  };

  return (
    <div className="table-block">
      {!readOnly && (
        <TableToolbar
          canEdit={sel != null}
          onAdd={() => setDlg({ index: -1, row: {} })}
          onEdit={edit}
          onRemove={remove}
        />
      )}
      {entries.length === 0 ? (
        <p className="muted empty-hint">(empty)</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>{cols.map((c) => <th key={c.name}>{c.label || c.name}</th>)}</tr>
            </thead>
            <tbody>
              {entries.map((row, i) => {
                const id = keyed ? row.__key__ : i;
                return (
                  <tr key={id} className={sel === id ? 'row-selected' : ''} onClick={() => setSel(sel === id ? null : id)}>
                    {cols.map((c) => <td key={c.name}>{cellValue(keyed && c.name === '__key__' ? row.__key__ : row[c.name])}</td>)}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      {dlg && (
        <RowDialog
          title={field.label || field.name}
          fields={cols}
          initial={dlg.row}
          onCancel={() => setDlg(null)}
          onSave={submit}
        />
      )}
      {confirmRemove && (
        <Modal title="Remove row" onClose={() => setConfirmRemove(false)}>
          <p>Remove {keyed && sel ? <strong>"{sel}"</strong> : 'this row'}?</p>
          <div className="btn-row">
            <button className="danger" onClick={doRemove}>Yes</button>
            <button onClick={() => setConfirmRemove(false)}>Cancel</button>
          </div>
        </Modal>
      )}
    </div>
  );
}

/** Table over a list (or Map) of scalar values with a single value column. */
function SimpleTable({ field, columns, rows, toEntry, onChange, readOnly, needKey }) {
  const [sel, setSel] = useState(null);
  const [dlg, setDlg] = useState(null);
  const [confirmRemove, setConfirmRemove] = useState(false);
  const [filter, setFilter] = useState('');
  const canModify = !readOnly;

  const q = filter.trim().toLowerCase();
  const displayed = rows
    .map((row, i) => ({ row, i }))
    .filter(({ row }) => {
      if (!q) return true;
      const key = row.__key__ == null ? '' : String(row.__key__);
      const val = row.__value__ == null ? '' : String(row.__value__);
      return key.toLowerCase().includes(q) || val.toLowerCase().includes(q);
    });

  const submit = (row) => {
    const key = needKey ? row.__key__ && row.__key__.trim() : true;
    if (!key) return;
    setDlg(null);
    setSel(null);
    let next;
    if (dlg.index < 0) {
      next = [...rows, row];
    } else {
      next = rows.map((r, i) => (i === dlg.index ? row : r));
    }
    onChange(next);
  };

  const remove = () => {
    if (sel == null) return;
    setConfirmRemove(true);
  };

  const doRemove = () => {
    setConfirmRemove(false);
    setSel(null);
    onChange(rows.filter((_, i) => i !== sel));
  };

  return (
    <div className="table-block">
      {!readOnly && (
        <TableToolbar
          canEdit={sel != null}
          onAdd={() => setDlg({ index: -1, row: {} })}
          onEdit={() => setDlg({ index: sel, row: { ...rows[sel] } })}
          onRemove={remove}
        />
      )}
      <input
        className="filter-input table-filter"
        type="text"
        placeholder="Filter by key or value…"
        value={filter}
        onChange={(e) => { setFilter(e.target.value); setSel(null); }}
      />
      {rows.length === 0 ? (
        <p className="muted empty-hint">(empty)</p>
      ) : displayed.length === 0 ? (
        <p className="muted empty-hint">No matches for "{filter.trim()}".</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>{columns.map((c) => <th key={c.name}>{c.label || c.name}</th>)}</tr>
            </thead>
            <tbody>
              {displayed.map(({ row, i }) => (
                <tr key={i} className={sel === i ? 'row-selected' : ''} onClick={() => setSel(sel === i ? null : i)}>
                  {columns.map((c) =>
                    c.name === '__key__' ? <td key={c.name}>{row.__key__}</td>
                      : <td key={c.name}>{cellValue(toEntry ? row.__value__ : row[c.name])}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {dlg && (
        <RowDialog
          title={field.label || field.name}
          fields={columns}
          initial={dlg.row}
          onCancel={() => setDlg(null)}
          onSave={submit}
        />
      )}
      {confirmRemove && (
        <Modal title="Remove row" onClose={() => setConfirmRemove(false)}>
          <p>Remove this row?</p>
          <div className="btn-row">
            <button className="danger" onClick={doRemove}>Yes</button>
            <button onClick={() => setConfirmRemove(false)}>Cancel</button>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ----------------------------------------------------------------------
// edit dialog
// ----------------------------------------------------------------------

function RowDialog({ title, fields, initial, onCancel, onSave }) {
  const [row, setRow] = useState(() => applyDefaults(initial || {}, fields));
  const hasKey = fields.some((f) => f.name === '__key__');
  const keyInvalid = hasKey && !((row.__key__ || '').trim());
  const setValue = (next) => setRow(next);

  return (
    <Modal title={title} onClose={onCancel}>
      {fields.map((f) =>
        f.name === '__key__' ? (
          <div className="field" key="__key__">
            <span className="label">Key</span>
            <input value={row.__key__ || ''} onChange={(e) => setValue({ ...row, __key__: e.target.value })} />
          </div>
        ) : (
          <FieldRow key={f.name} field={f} value={row} onChange={setValue} />
        ))}
      <div className="btn-row">
        <button className="primary" disabled={keyInvalid} onClick={() => onSave(row)}>Save</button>
        <button onClick={onCancel}>Cancel</button>
      </div>
    </Modal>
  );
}

// ----------------------------------------------------------------------
// simple field rows (unchanged from the flat form)
// ----------------------------------------------------------------------

function FieldRow({ field, value, onChange, readOnly, doc, onBaseChange, keyNameEditable = true }) {
  const current = readPath(value, field.name);
  const label = field.label || field.name;

  const set = (next) => onChange(setPath(value, field.name, next));

  if (
    field.visibleField
    && String(readPath(value, field.visibleField) ?? '').toLowerCase()
      !== String(field.visibleValue).toLowerCase()
  ) {
    return null;
  }

  if (field.type === 'dag') {
    return (
      <div className="field-block">
        <span className="label">{label} (flow)</span>
        <DagEditor
          nodeField={siblingNodeField(doc, field.name)}
          edgeField={field.name}
          doc={doc}
          onDocChange={onBaseChange}
          readOnly={readOnly}
        />
      </div>
    );
  }

  if (field.type === 'object') {
    const children = field.children || [];
    const hasReferencePicker = children.some((c) => c.picker === 'datasetConstraint');
    return (
      <div className="field-block">
        <span className="label">{label}</span>
        <div className="box">
          {children.length === 0 && <span className="muted">(empty)</span>}
          {children.map((child) => {
            if (child.name === 'constraintName' && hasReferencePicker) return null;
            if (child.picker === 'datasetConstraint')
              return <ReferenceEditor key={child.name} value={current || {}} onChange={set} readOnly={readOnly} />;
            return (
              <FieldRow
                key={child.name}
                field={child}
                value={current || {}}
                onChange={set}
                doc={doc}
                onBaseChange={onBaseChange}
                readOnly={readOnly}
                keyNameEditable={keyNameEditable}
              />
            );
          })}
        </div>
      </div>
    );
  }

  if (field.type === 'list') {
    return (
      <div className="field-block">
        <span className="label">{label}</span>
        <ListEditor
          item={field.item}
          value={current}
          onChange={set}
          readOnly={readOnly}
          doc={doc}
          onBaseChange={onBaseChange}
        />
      </div>
    );
  }

  if (field.type === 'map') {
    return (
      <div className="field-block">
        <span className="label">{label}</span>
        <MapEditor
          field={field}
          value={current}
          onChange={set}
          readOnly={readOnly}
          doc={doc}
          onBaseChange={onBaseChange}
        />
      </div>
    );
  }

  return <ScalarField field={field} value={current} onChange={set} readOnly={readOnly} keyNameEditable={keyNameEditable} />;
}

/** Read-only pair Data Set Key Name + Constraint Name backed by the constraint picker "…" button. */
function ReferenceEditor({ value, onChange, readOnly }) {
  const ref = value && typeof value === 'object' ? value : {};
  const [open, setOpen] = useState(false);
  const { dataSetSummaryProvider } = React.useContext(FormContext);
  const apply = (sel) => onChange({ ...ref, dataSetKeyName: sel.dataSetKeyName, constraintName: sel.constraintName });

  return (
    <>
      <div className="scalar-row">
        <span className="label">Data Set Key Name</span>
        <div className="scalar-input-wrap">
          <input value={ref.dataSetKeyName || ''} readOnly placeholder="Choose a data set" />
          {!readOnly && (
            <button
              className="square picker-btn"
              title="Choose data set and constraint"
              onClick={() => setOpen(true)}
            >…</button>
          )}
        </div>
      </div>
      <div className="scalar-row">
        <span className="label">Constraint Name</span>
        <input value={ref.constraintName || ''} readOnly placeholder="Choose a constraint" />
      </div>
      {open && dataSetSummaryProvider && (
        <ConstraintPickerModal
          summaryProvider={dataSetSummaryProvider}
          onClose={() => setOpen(false)}
          onApply={apply}
        />
      )}
    </>
  );
}

function ScalarField({ field, value, onChange, readOnly, keyNameEditable = true }) {
  const isKeyName = field.keyName && !keyNameEditable;
  const disabled = readOnly || field.readOnly || isKeyName;
  const label = field.label || field.name;
  const title = field.description || '';
  const inputRef = useRef(null);
  const { uniqueByField, rootDoc, scriptSummaryProvider, nameSpaceSummaryProvider, catalogProviders } = React.useContext(FormContext);
  const uniqueCheck = field.uniqueAcrossKind ? (uniqueByField[field.name] || null) : null;
  const [invalid, setInvalid] = useState(false);
  const [errorVisible, setErrorVisible] = useState(false);
  const [columnsOpen, setColumnsOpen] = useState(false);
  const [scriptKeyOpen, setScriptKeyOpen] = useState(false);
  const [nameSpaceOpen, setNameSpaceOpen] = useState(false);
  const [namedOpen, setNamedOpen] = useState(false);
  const [clearOpen, setClearOpen] = useState(false);

  const checkUnique = () => {
    if (!uniqueCheck) return;
    const text = stringify(value);
    if (text && uniqueCheck.includes(text)) {
      setInvalid(true);
      setErrorVisible(true);
      window.setTimeout(() => setErrorVisible(false), 5000);
      window.setTimeout(() => inputRef.current && inputRef.current.focus(), 0);
    } else {
      setInvalid(false);
    }
  };

  if (field.type === 'code') {
    return (
      <div className="scalar-row scalar-row--code">
        <span className="label" title={title}>
          {label}
          {field.required && <span className="req">*</span>}
        </span>
        <CodeEditor value={value || ''} onChange={onChange} readOnly={disabled} />
      </div>
    );
  }

  if (field.picker === 'columns') {
    const columns = rootDoc && Array.isArray(rootDoc.columnSchema) ? rootDoc.columnSchema : [];
    return (
      <div className="scalar-row">
        <span className="label" title={title}>
          {label}
          {field.required && <span className="req">*</span>}
        </span>
        <div className="scalar-input-wrap">
          <input value={stringify(value)} readOnly placeholder="Choose columns" />
          {!readOnly && (
            <button className="square picker-btn" title="Choose columns" onClick={() => setColumnsOpen(true)}>…</button>
          )}
        </div>
        {columnsOpen && (
          <ColumnPickerModal columns={columns} value={value} onClose={() => setColumnsOpen(false)} onApply={onChange} />
        )}
      </div>
    );
  }

  if (field.picker === 'nameSpace') {
    return (
      <div className="scalar-row">
        <span className="label" title={title}>
          {label}
          {field.required && <span className="req">*</span>}
        </span>
        <div className="scalar-input-wrap">
          <input value={stringify(value)} readOnly placeholder="Choose a name space" />
          {!readOnly && (
            <>
              <button className="square picker-btn" title="Choose name space" onClick={() => setNameSpaceOpen(true)}>…</button>
              {field.clearable && (
                <button
                  className="square picker-btn clear-btn"
                  title="Clear field"
                  onClick={() => setClearOpen(true)}
                >×</button>
              )}
            </>
          )}
        </div>
        {nameSpaceOpen && nameSpaceSummaryProvider && (
          <NameSpacePickerModal
            summaryProvider={nameSpaceSummaryProvider}
            onClose={() => setNameSpaceOpen(false)}
            onApply={onChange}
          />
        )}
        {clearOpen && (
          <Modal title="Clear field" onClose={() => setClearOpen(false)}>
            <p>Clear <strong>{label}</strong>?</p>
            <div className="btn-row">
              <button
                className="danger"
                onClick={() => {
                  onChange(null);
                  setClearOpen(false);
                }}
              >Yes</button>
              <button onClick={() => setClearOpen(false)}>Cancel</button>
            </div>
          </Modal>
        )}
      </div>
    );
  }

  const namedPicker = NAMED_ITEM_PICKERS[field.picker];
  if (namedPicker) {
    const catalogProvider = catalogProviders[field.picker];
    return (
      <div className="scalar-row">
        <span className="label" title={title}>
          {label}
          {field.required && <span className="req">*</span>}
        </span>
        <div className="scalar-input-wrap">
          <input value={stringify(value)} readOnly placeholder={namedPicker.placeholder} />
          {!readOnly && (
            <>
              <button className="square picker-btn" title={namedPicker.title} onClick={() => setNamedOpen(true)}>…</button>
              {field.clearable && (
                <button
                  className="square picker-btn clear-btn"
                  title="Clear field"
                  onClick={() => setClearOpen(true)}
                >×</button>
              )}
            </>
          )}
        </div>
        {namedOpen && catalogProvider && (
          <NamedItemPickerModal
            title={namedPicker.title}
            placeholder={namedPicker.placeholder}
            summaryProvider={catalogProvider}
            idField={namedPicker.idField}
            onClose={() => setNamedOpen(false)}
            onApply={onChange}
          />
        )}
        {clearOpen && (
          <Modal title="Clear field" onClose={() => setClearOpen(false)}>
            <p>Clear <strong>{label}</strong>?</p>
            <div className="btn-row">
              <button
                className="danger"
                onClick={() => {
                  onChange(null);
                  setClearOpen(false);
                }}
              >Yes</button>
              <button onClick={() => setClearOpen(false)}>Cancel</button>
            </div>
          </Modal>
        )}
      </div>
    );
  }

  if (field.picker === 'scriptKey') {
    return (
      <div className="scalar-row">
        <span className="label" title={title}>
          {label}
          {field.required && <span className="req">*</span>}
        </span>
        <div className="scalar-input-wrap">
          <input value={stringify(value)} readOnly placeholder="Choose a script" />
          {!readOnly && (
            <>
              <button className="square picker-btn" title="Choose script" onClick={() => setScriptKeyOpen(true)}>…</button>
              {field.clearable && (
                <button
                  className="square picker-btn clear-btn"
                  title="Clear field"
                  onClick={() => setClearOpen(true)}
                >×</button>
              )}
            </>
          )}
        </div>
        {scriptKeyOpen && scriptSummaryProvider && (
          <ScriptKeyPreviewModal
            summaryProvider={scriptSummaryProvider}
            onClose={() => setScriptKeyOpen(false)}
            onApply={onChange}
          />
        )}
        {clearOpen && (
          <Modal title="Clear field" onClose={() => setClearOpen(false)}>
            <p>Clear <strong>{label}</strong>?</p>
            <div className="btn-row">
              <button
                className="danger"
                onClick={() => {
                  onChange(null);
                  setClearOpen(false);
                }}
              >Yes</button>
              <button onClick={() => setClearOpen(false)}>Cancel</button>
            </div>
          </Modal>
        )}
      </div>
    );
  }

  if (field.enumValues && field.enumValues.length > 0) {
    return (
      <div className={`scalar-row${invalid ? ' field-invalid' : ''}`}>
        <label className="label" title={title}>
          {label}
          {field.required && <span className="req">*</span>}
        </label>
        <select
          value={value == null ? '' : String(value)}
          disabled={disabled}
          onChange={(e) => {
            if (invalid) setInvalid(false);
            onChange(e.target.value === '' ? null : e.target.value);
          }}
        >
          <option value="">—</option>
          {field.enumValues.map((v) => <option key={v} value={v}>{v}</option>)}
        </select>
      </div>
    );
  }

  if (field.type === 'boolean') {
    return (
      <div className="scalar-row">
        <span className="label" title={title}>{label}</span>
        <input
          type="checkbox"
          checked={!!value}
          disabled={disabled}
          ref={inputRef}
          onChange={(e) => onChange(e.target.checked)}
        />
      </div>
    );
  }

  const inputType = field.type === 'integer' ? 'number'
    : field.type === 'double' ? 'number'
      : 'text';

  return (
    <div className={`scalar-row${invalid ? ' field-invalid' : ''}`}>
      <label className="label" title={title}>
        {label}
        {field.required && <span className="req">*</span>}
      </label>
      <div className="scalar-input-wrap">
        <input
          type={inputType}
          step={field.type === 'double' ? 'any' : undefined}
          value={stringify(value)}
          disabled={disabled}
          ref={inputRef}
          placeholder={field.keyName ? 'e.g. myKeyName' : ''}
          onBlur={checkUnique}
          onChange={(e) => {
            if (invalid) setInvalid(false);
            if (field.type === 'integer') {
              const n = parseInt(e.target.value, 10);
              onChange(Number.isNaN(n) ? null : n);
            } else if (field.type === 'double') {
              const n = parseFloat(e.target.value);
              onChange(Number.isNaN(n) ? null : n);
            } else {
              onChange(e.target.value);
            }
          }}
        />
        {errorVisible && (
          <div className="field-error-popup">
            {label} "{stringify(value)}" already exists; keys must be unique.
          </div>
        )}
      </div>
    </div>
  );
}

function stringify(v) {
  if (v === null || v === undefined) return '';
  if (typeof v === 'boolean') return v ? 'true' : 'false';
  return String(v);
}

function ListEditor({ item, value, onChange, readOnly, doc, onBaseChange }) {
  const list = Array.isArray(value) ? value : [];
  const update = (entry, idx) => {
    const next = list.slice();
    next[idx] = entry;
    onChange(next);
  };
  const remove = (idx) => onChange(list.filter((_, i) => i !== idx));
  const add = () => onChange([...list, defaultFor(item)]);

  return (
    <div className="box">
      {list.length === 0 && <span className="muted">(empty {readOnly ? 'list' : 'list — click add'})</span>}
      {list.map((entry, idx) => (
        <div className="box-row" key={idx}>
          {item && (item.type === 'object' ? (
            item.children && item.children.length > 0 ? (
              <div className="box">
                {item.children.map((child) => (
                  <FieldRow
                    key={`${idx}-${child.name}`}
                    field={child}
                    value={entry || {}}
                    onChange={(next) => update(next, idx)}
                    readOnly={readOnly}
                    doc={doc}
                    onBaseChange={onBaseChange}
                  />
                ))}
              </div>
            ) : (
              <ScalarField
                field={item}
                value={entry}
                onChange={(next) => update(next, idx)}
                readOnly={readOnly}
              />
            )
          ) : (
            <ScalarField
              field={item}
              value={entry}
              onChange={(next) => update(next, idx)}
              readOnly={readOnly}
            />
          ))}
          {!readOnly && <button className="danger small" onClick={() => remove(idx)}>Remove</button>}
        </div>
      ))}
      {!readOnly && <button className="small" onClick={add}>+ Add</button>}
    </div>
  );
}

function MapEditor({ field, value, onChange, readOnly, doc, onBaseChange }) {
  const obj = value && typeof value === 'object' ? value : {};
  const keys = Object.keys(obj);
  const valueChildren = field.children || [];
  const isObjectMap = valueChildren.length > 1 || (valueChildren.length === 1 && valueChildren[0].name !== 'value');

  if (!isObjectMap) {
    const valueChild = valueChildren[0];
    const valueField = {
      name: '__value__',
      type: valueChild ? valueChild.type || 'string' : 'string',
      label: (valueChild && valueChild.label) || 'Value',
    };
    const rows = keys.map((k) => ({ __key__: k, __value__: obj[k] }));
    return (
      <SimpleTable
        field={field}
        columns={[{ name: '__key__', type: 'string', label: 'Key' }, valueField]}
        needKey
        rows={rows}
        toEntry={(row) => row.__value__}
        onChange={(nextRows) => {
          const next = {};
          for (const row of nextRows) if (row.__key__ && row.__key__.trim()) next[row.__key__] = row.__value__;
          onChange(next);
        }}
        readOnly={readOnly}
      />
    );
  }

  const updateEntry = (key, nextEntry) => {
    const next = { ...obj };
    next[key] = nextEntry || {};
    onChange(next);
  };
  const removeKey = (key) => {
    const next = { ...obj };
    delete next[key];
    onChange(next);
  };
  const addKey = () => {
    const base = getOrCreate('newKey', obj, isObjectMap, valueChildren);
    const next = { ...obj };
    next[defaultMapKey(obj)] = base;
    onChange(next);
  };

  return (
    <div className="box">
      {keys.length === 0 && <span className="muted">(empty)</span>}
      {keys.map((key) => {
        const entry = obj[key];
        return (
          <div className="box-row row-stack" key={key}>
            <div className="map-head">
              <input
                value={key}
                disabled={readOnly}
                onChange={(e) => {
                  const next = { ...obj };
                  next[e.target.value] = entry;
                  delete next[key];
                  onChange(next);
                }}
              />
              {!readOnly && <button className="danger small" onClick={() => removeKey(key)}>Remove</button>}
            </div>
            <div className="box">
              {valueChildren.map((child) => (
                <FieldRow
                  key={`${key}-${child.name}`}
                  field={child}
                  value={entry || {}}
                  onChange={(next) => updateEntry(key, next)}
                  readOnly={readOnly}
                  doc={doc}
                  onBaseChange={onBaseChange}
                />
              ))}
            </div>
          </div>
        );
      })}
      {!readOnly && <button className="small" onClick={addKey}>+ Add entry</button>}
    </div>
  );
}

function defaultMapKey(obj) {
  let i = 1;
  while (obj[`key${i}`]) i += 1;
  return `key${i}`;
}

function getOrCreate(key, obj, isObjectMap, children) {
  const base = obj[key];
  if (base !== undefined) return base;
  if (isObjectMap) {
    const entry = {};
    if (children) {
      for (const child of children) entry[child.name] = defaultFor(child);
    }
    return entry;
  }
  return defaultFor(children && children[0]);
}

function defaultFor(field) {
  if (!field) return '';
  if (field.enumDefault) return field.enumDefault;
  switch (field.type) {
    case 'boolean': return false;
    case 'integer': return 0;
    case 'double': return 0;
    case 'object': return { enabled: true };
    case 'list': return [];
    case 'map': return {};
    default: return '';
  }
}

/** Pre-fills declarative defaults (e.g. constraint type = primary) for new dialog rows. */
function applyDefaults(row, fields) {
  const next = { ...row };
  for (const f of fields || []) {
    if (next[f.name] === undefined && f.enumDefault != null) next[f.name] = f.enumDefault;
  }
  return next;
}

/**
 * Keeps DataSet constraints consistent: checkExpr is kept only for type "check"
 * and reference only for type "foreign"; the hidden counterpart is nulled on save.
 */
function normalizeDataSetConstraints(root) {
  if (!root || !root.constraints || typeof root.constraints !== 'object') return root;
  let changed = false;
  const nextConstraints = {};
  for (const [key, c] of Object.entries(root.constraints)) {
    if (!c || typeof c !== 'object') {
      nextConstraints[key] = c;
      continue;
    }
    const type = c.type == null ? '' : String(c.type);
    let nc = c;
    if (type !== 'check' && c.checkExpr != null) nc = { ...nc, checkExpr: null };
    if (type !== 'foreign' && c.reference != null) nc = { ...nc, reference: null };
    if (nc !== c) changed = true;
    nextConstraints[key] = nc;
  }
  return changed ? { ...root, constraints: nextConstraints } : root;
}

function readPath(root, name) {
  return root ? root[name] : undefined;
}

function setPath(root, name, next) {
  return { ...(root || {}), [name]: next };
}

/** Finds a sibling field that is a list of objects with `keyName` (or `name`) to serve as DAG nodes. */
function siblingNodeField(doc, dagFieldName) {
  if (!doc) return 'scenarioActs';
  for (const key of Object.keys(doc)) {
    if (key === dagFieldName) continue;
    const v = doc[key];
    if (Array.isArray(v) && v.length > 0 && v[0] && typeof v[0] === 'object'
        && (v[0].keyName != null || v[0].name)) {
      return key;
    }
  }
  return null;
}