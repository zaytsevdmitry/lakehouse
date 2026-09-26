import React, { useEffect, useMemo, useState } from 'react';
import Modal from './Modal';
import CodeEditor from './CodeEditor';

/**
 * Picker for a DataSet `sources` Key: lists every DataSet `keyName` known in
 * the workspace tree. Save writes the chosen key name, Cancel leaves the field
 * as is.
 */
export function DataSetKeyPickerModal({ summaryProvider, onClose, onApply }) {
  const [dataSets, setDataSets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    let alive = true;
    Promise.resolve(summaryProvider()).then((list) => {
      if (!alive) return;
      setDataSets(list || []);
      setLoading(false);
    });
    return () => {
      alive = false;
    };
  }, [summaryProvider]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return dataSets;
    return dataSets.filter((ds) => (ds.keyName || '').toLowerCase().includes(q));
  }, [dataSets, query]);

  return (
    <Modal title="Choose data set" onClose={onClose}>
      <input
        className="filter-input"
        type="text"
        autoFocus
        value={query}
        placeholder="Search data set key name…"
        onChange={(e) => setQuery(e.target.value)}
      />
      <div className="picker-list">
        {loading ? (
          <p className="muted">Loading data sets…</p>
        ) : filtered.length === 0 ? (
          <p className="muted">No matching data sets.</p>
        ) : (
          filtered.map((ds) => {
            const key = ds.keyName || '';
            return (
              <button
                key={key}
                className={`picker-row${selected === ds ? ' selected' : ''}`}
                onClick={() => setSelected(ds)}
              >
                <span>{key}</span>
              </button>
            );
          })
        )}
      </div>
      <div className="btn-row">
        <button
          className="primary"
          disabled={!selected}
          onClick={() => {
            if (selected) {
              onApply(selected.keyName);
              onClose();
            }
          }}
        >
          Save
        </button>
        <button onClick={onClose}>Cancel</button>
      </div>
    </Modal>
  );
}

/**
 * Generic picker for read-only "catalog" fields (Data Source Key Name, Task
 * Template, Task Execution Service Group Name, Driver Key Name): a searchable
 * list of the workspace documents of the referenced kind. Save writes the
 * {@code idField} of the chosen item, Cancel leaves the field as is.
 */
export function NamedItemPickerModal({
  title,
  placeholder,
  summaryProvider,
  idField,
  descriptionField = 'description',
  onClose,
  onApply,
}) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    let alive = true;
    Promise.resolve(summaryProvider()).then((list) => {
      if (!alive) return;
      setItems(list || []);
      setLoading(false);
    });
    return () => {
      alive = false;
    };
  }, [summaryProvider]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return items;
    return items.filter(
      (it) =>
        String(it[idField] || '').toLowerCase().includes(q) ||
        String(it[descriptionField] || '').toLowerCase().includes(q),
    );
  }, [items, query, idField, descriptionField]);

  return (
    <Modal title={title} onClose={onClose}>
      <input
        className="filter-input"
        type="text"
        autoFocus
        value={query}
        placeholder={placeholder}
        onChange={(e) => setQuery(e.target.value)}
      />
      <div className="picker-list">
        {loading ? (
          <p className="muted">Loading…</p>
        ) : filtered.length === 0 ? (
          <p className="muted">No matching items.</p>
        ) : (
          filtered.map((it) => {
            const id = String(it[idField] || '');
            const desc = String(it[descriptionField] || '');
            return (
              <button
                key={id}
                className={`picker-row${selected === it ? ' selected' : ''}`}
                onClick={() => setSelected(it)}
              >
                <span>{id}</span>
                {desc && <span className="muted small picker-row-desc">{desc}</span>}
              </button>
            );
          })
        )}
      </div>
      <div className="btn-row">
        <button
          className="primary"
          disabled={!selected}
          onClick={() => {
            if (selected) {
              onApply(selected[idField]);
              onClose();
            }
          }}
        >
          Save
        </button>
        <button onClick={onClose}>Cancel</button>
      </div>
    </Modal>
  );
}

/**
 * Picker for the Script `key` field: the full script list on the left and a
 * read-only highlighted code viewer on the right. Clicking a key previews its
 * value; Save writes the chosen key and closes, Cancel leaves the field as is.
 */
export function ScriptKeyPreviewModal({ summaryProvider, onClose, onApply }) {
  const [scripts, setScripts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    let alive = true;
    Promise.resolve(summaryProvider()).then((list) => {
      if (!alive) return;
      setScripts(list || []);
      setLoading(false);
    });
    return () => {
      alive = false;
    };
  }, [summaryProvider]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return scripts;
    return scripts.filter((s) => (s.key || '').toLowerCase().includes(q));
  }, [scripts, query]);

  const select = (s) => setSelected(s);

  return (
    <Modal title="Choose script" onClose={onClose} className="modal--code">
      <input
        className="filter-input"
        type="text"
        autoFocus
        value={query}
        placeholder="Search script key…"
        onChange={(e) => setQuery(e.target.value)}
      />
      <div className="script-key-picker">
        <div className="script-key-list">
          {loading ? (
            <p className="muted">Loading scripts…</p>
          ) : filtered.length === 0 ? (
            <p className="muted">No matching scripts.</p>
          ) : (
            filtered.map((s) => {
              const key = s.key || '';
              return (
                <button
                  key={key}
                  className={`picker-row picker-row--script${selected === s ? ' selected' : ''}`}
                  onClick={() => select(s)}
                >
                  <span>{key}</span>
                </button>
              );
            })
          )}
        </div>
        <div className="script-key-preview">
          <CodeEditor value={(selected && selected.value) || ''} readOnly />
        </div>
      </div>
      <div className="btn-row">
        <button
          className="primary"
          disabled={!selected}
          onClick={() => {
            if (selected) {
              onApply(selected.key);
              onClose();
            }
          }}
        >
          Save
        </button>
        <button onClick={onClose}>Cancel</button>
      </div>
    </Modal>
  );
}

/**
 * Pickers opened from the DataSet Constraints editor.
 *
 * ConstraintPickerModal lets an author point a foreign key reference at an
 * existing PRIMARY/UNIQUE constraint of another data set, filling both the
 * Data Set Key Name and Constraint Name fields on Save. Cancel restores the
 * previous (unchanged) values.
 *
 * ColumnPickerModal selects the governed columns of the current data set via
 * check boxes; Save joins the chosen column names with a comma, Cancel leaves
 * the field untouched.
 */
export function ConstraintPickerModal({ summaryProvider, onClose, onApply }) {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    let alive = true;
    Promise.resolve(summaryProvider()).then((list) => {
      if (!alive) return;
      const flattened = [];
      for (const ds of list || []) {
        for (const c of ds.constraints || []) {
          const type = c && c.type ? String(c.type) : 'primary';
          if (type === 'primary' || type === 'unique')
            flattened.push({ dataSetKeyName: ds.keyName, constraintName: c.key });
        }
      }
      setRows(flattened);
      setLoading(false);
    });
    return () => {
      alive = false;
    };
  }, [summaryProvider]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter(
      (r) => r.dataSetKeyName.toLowerCase().includes(q) || r.constraintName.toLowerCase().includes(q),
    );
  }, [rows, query]);

  return (
    <Modal title="Choose referenced data set constraint" onClose={onClose} wide>
      <input
        className="filter-input"
        type="text"
        autoFocus
        value={query}
        placeholder="Search by data set or constraint name…"
        onChange={(e) => setQuery(e.target.value)}
      />
      <div className="picker-grid-head">
        <span>Data Set Key Name</span>
        <span>Constraint Name</span>
      </div>
      <div className="picker-list">
        {loading ? (
          <p className="muted">Loading data sets…</p>
        ) : filtered.length === 0 ? (
          <p className="muted">No matching primary/unique constraints.</p>
        ) : (
          filtered.map((r) => (
            <button
              key={`${r.dataSetKeyName}/${r.constraintName}`}
              className={`picker-row${selected === r ? ' selected' : ''}`}
              onClick={() => setSelected(r)}
            >
              <span>{r.dataSetKeyName}</span>
              <span>{r.constraintName}</span>
            </button>
          ))
        )}
      </div>
      <div className="btn-row">
        <button
          className="primary"
          disabled={!selected}
          onClick={() => {
            if (!selected) return;
            onApply(selected);
            onClose();
          }}
        >
          Save
        </button>
        <button onClick={onClose}>Cancel</button>
      </div>
    </Modal>
  );
}

const columnName = (c) => (c && typeof c === 'object' ? c.name : c);
const columnLabel = (c) => {
  const name = columnName(c);
  return name == null ? '' : String(name);
};

export function ColumnPickerModal({ columns, value, onClose, onApply }) {
  const names = useMemo(() => (Array.isArray(columns) ? columns.filter(columnLabel) : []), [columns]);
  const initial = useMemo(() => {
    const set = new Set();
    String(value || '')
      .split(',')
      .forEach((s) => {
        const t = s.trim();
        if (t) set.add(t);
      });
    return set;
  }, [value]);
  const [selected, setSelected] = useState(new Set(initial));
  const [query, setQuery] = useState('');

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return names;
    return names.filter((c) => columnLabel(c).toLowerCase().includes(q));
  }, [names, query]);

  const toggle = (name) => {
    const next = new Set(selected);
    if (next.has(name)) next.delete(name);
    else next.add(name);
    setSelected(next);
  };

  const apply = () => {
    const picked = names.filter((c) => selected.has(columnLabel(c))).map(columnLabel);
    onApply(picked.join(','));
    onClose();
  };

  return (
    <Modal title="Choose columns" onClose={onClose} wide>
      <input
        className="filter-input"
        type="text"
        autoFocus
        value={query}
        placeholder="Search columns…"
        onChange={(e) => setQuery(e.target.value)}
      />
      <div className="picker-list">
        {filtered.length === 0 ? (
          <p className="muted">No columns in this data set.</p>
        ) : (
          filtered.map((c) => {
            const name = columnLabel(c);
            return (
              <label key={name} className={`picker-row picker-check${selected.has(name) ? ' selected' : ''}`}>
                <input type="checkbox" checked={selected.has(name)} onChange={() => toggle(name)} />
                <span>{name}</span>
              </label>
            );
          })
        )}
      </div>
      <div className="btn-row">
        <button className="primary" onClick={apply}>Save</button>
        <button onClick={onClose}>Cancel</button>
      </div>
    </Modal>
  );
}